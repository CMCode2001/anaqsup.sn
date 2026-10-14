package sn.anaqsup.portail.referentiel;

/** Lecture du référentiel par les autres modules (ex. statistiques). */
public interface ReferentielQueries {

    long countFavorableHabilitations();

    long countFavorableAccreditations();
}
