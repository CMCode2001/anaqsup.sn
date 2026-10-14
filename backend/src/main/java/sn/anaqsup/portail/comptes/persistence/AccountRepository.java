package sn.anaqsup.portail.comptes.persistence;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import sn.anaqsup.portail.comptes.Permission;

/** Comptes et rôles du back-office (SQL explicite, comme le reste de l'application). */
@Repository
public class AccountRepository {

    public record RoleRow(UUID id, String name, Set<Permission> permissions, boolean system, int accountCount) {
    }

    public record AccountRow(UUID id, String email, String displayName, String passwordHash, UUID roleId,
                             String roleName, boolean roleSystem, Set<Permission> permissions,
                             boolean mustChangePassword, boolean enabled, @Nullable Instant lastLoginAt,
                             Instant createdAt) {
    }

    private static final String ACCOUNT_SELECT = """
            SELECT a.id, a.email, a.display_name, a.password_hash, a.role_id, r.name AS role_name,
                   r.system AS role_system, r.permissions, a.must_change_password, a.enabled,
                   a.last_login_at, a.created_at
            FROM admin_account a JOIN admin_role r ON r.id = a.role_id""";

    private final JdbcClient jdbc;

    AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------------------------------- comptes

    public Optional<AccountRow> findByEmail(String email) {
        return jdbc.sql(ACCOUNT_SELECT + " WHERE lower(a.email) = lower(:email)")
                .param("email", email).query((rs, n) -> account(rs)).optional();
    }

    public Optional<AccountRow> findById(UUID id) {
        return jdbc.sql(ACCOUNT_SELECT + " WHERE a.id = :id").param("id", id).query((rs, n) -> account(rs)).optional();
    }

    public List<AccountRow> accounts() {
        return jdbc.sql(ACCOUNT_SELECT + " ORDER BY a.display_name, a.email").query((rs, n) -> account(rs)).list();
    }

    public long countAccounts() {
        return jdbc.sql("SELECT count(*) FROM admin_account").query(Long.class).single();
    }

    /** Comptes actifs ayant le rôle Administrateur (le seul rôle système). */
    public long countEnabledAdministrators() {
        return jdbc.sql("""
                        SELECT count(*) FROM admin_account a JOIN admin_role r ON r.id = a.role_id
                        WHERE r.system AND a.enabled""")
                .query(Long.class).single();
    }

    public boolean emailExists(String email) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM admin_account WHERE lower(email) = lower(:email))")
                .param("email", email).query(Boolean.class).single();
    }

    public UUID insertAccount(String email, String displayName, String passwordHash, UUID roleId) {
        return jdbc.sql("""
                        INSERT INTO admin_account (email, display_name, password_hash, role_id, must_change_password)
                        VALUES (btrim(:email), btrim(:name), :hash, :role, true)
                        RETURNING id""")
                .param("email", email).param("name", displayName).param("hash", passwordHash).param("role", roleId)
                .query(UUID.class).single();
    }

    public void updateAccount(UUID id, String displayName, UUID roleId, boolean enabled) {
        jdbc.sql("""
                        UPDATE admin_account SET display_name = btrim(:name), role_id = :role, enabled = :enabled,
                                                 updated_at = now()
                        WHERE id = :id""")
                .param("id", id).param("name", displayName).param("role", roleId).param("enabled", enabled).update();
    }

    public void updatePassword(UUID id, String passwordHash, boolean mustChangePassword) {
        jdbc.sql("""
                        UPDATE admin_account SET password_hash = :hash, must_change_password = :mustChange,
                                                 updated_at = now()
                        WHERE id = :id""")
                .param("id", id).param("hash", passwordHash).param("mustChange", mustChangePassword).update();
    }

    public void touchLastLogin(UUID id) {
        jdbc.sql("UPDATE admin_account SET last_login_at = now() WHERE id = :id").param("id", id).update();
    }

    public void deleteAccount(UUID id) {
        jdbc.sql("DELETE FROM admin_account WHERE id = :id").param("id", id).update();
    }

    // ------------------------------------------------------------------------------------------ rôles

    private static final String ROLE_SELECT = """
            SELECT r.id, r.name, r.permissions, r.system,
                   (SELECT count(*) FROM admin_account a WHERE a.role_id = r.id) AS account_count
            FROM admin_role r""";

    public List<RoleRow> roles() {
        // L'Administrateur d'abord, puis les autres rôles par ordre alphabétique.
        return jdbc.sql(ROLE_SELECT + " ORDER BY r.system DESC, r.name").query((rs, n) -> role(rs)).list();
    }

    public Optional<RoleRow> findRole(UUID id) {
        return jdbc.sql(ROLE_SELECT + " WHERE r.id = :id").param("id", id).query((rs, n) -> role(rs)).optional();
    }

    public UUID administratorRoleId() {
        return jdbc.sql("SELECT id FROM admin_role WHERE system").query(UUID.class).single();
    }

    public boolean roleNameExists(String name, @Nullable UUID exceptId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM admin_role
                                       WHERE lower(btrim(name)) = lower(btrim(:name))
                                         AND (CAST(:except AS uuid) IS NULL OR id <> CAST(:except AS uuid)))""")
                .param("name", name).param("except", exceptId).query(Boolean.class).single();
    }

    public UUID insertRole(String name, Set<Permission> permissions) {
        return jdbc.sql("INSERT INTO admin_role (name, permissions) VALUES (btrim(:name), :permissions) RETURNING id")
                .param("name", name).param("permissions", names(permissions)).query(UUID.class).single();
    }

    public void updateRole(UUID id, String name, Set<Permission> permissions) {
        jdbc.sql("UPDATE admin_role SET name = btrim(:name), permissions = :permissions WHERE id = :id AND NOT system")
                .param("id", id).param("name", name).param("permissions", names(permissions)).update();
    }

    public void deleteRole(UUID id) {
        jdbc.sql("DELETE FROM admin_role WHERE id = :id AND NOT system").param("id", id).update();
    }

    // ---------------------------------------------------------------------------------------- mapping

    private static String[] names(Set<Permission> permissions) {
        return permissions.stream().map(Enum::name).sorted().toArray(String[]::new);
    }

    private static Set<Permission> permissions(ResultSet rs) throws SQLException {
        Array array = rs.getArray("permissions");
        Set<Permission> result = EnumSet.noneOf(Permission.class);
        if (array != null) {
            Arrays.stream((String[]) array.getArray()).map(Permission::valueOf).forEach(result::add);
        }
        return result;
    }

    private static AccountRow account(ResultSet rs) throws SQLException {
        return new AccountRow(rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("display_name"),
                rs.getString("password_hash"), rs.getObject("role_id", UUID.class), rs.getString("role_name"),
                rs.getBoolean("role_system"), permissions(rs), rs.getBoolean("must_change_password"),
                rs.getBoolean("enabled"), instant(rs, "last_login_at"), instant(rs, "created_at"));
    }

    private static RoleRow role(ResultSet rs) throws SQLException {
        return new RoleRow(rs.getObject("id", UUID.class), rs.getString("name"), permissions(rs),
                rs.getBoolean("system"), rs.getInt("account_count"));
    }

    private static @Nullable Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
