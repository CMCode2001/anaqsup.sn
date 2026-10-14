import { EditorContent, useEditor, useEditorState } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import {
  BoldIcon, Heading2Icon, Heading3Icon, ItalicIcon, Link2Icon, ListIcon, ListOrderedIcon, QuoteIcon, Redo2Icon,
  UnderlineIcon, Undo2Icon, type LucideIcon,
} from "lucide-react";

import { cn } from "@anaqsup/ui/lib/utils";

/**
 * Éditeur de texte du back-office : uniquement ce que le site sait afficher (titres, gras, italique, souligné,
 * listes, citations, liens). Le serveur assainit de toute façon le HTML enregistré (liste blanche).
 */
export function RichTextEditor({ value, onChange, label }: { value: string; onChange: (html: string) => void; label: string }) {
  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [2, 3] },
        code: false,
        codeBlock: false,
        horizontalRule: false,
        link: { openOnClick: false, autolink: true, protocols: ["https", "http", "mailto"], defaultProtocol: "https" },
      }),
    ],
    content: value,
    editorProps: {
      attributes: {
        "aria-label": label,
        class: "prose-anaqsup min-h-64 px-5 py-4 focus:outline-none",
      },
    },
    onUpdate: ({ editor: e }) => onChange(e.getHTML()),
    immediatelyRender: true,
  });

  const state = useEditorState({
    editor,
    selector: ({ editor: e }) => ({
      bold: e.isActive("bold"), italic: e.isActive("italic"), underline: e.isActive("underline"),
      h2: e.isActive("heading", { level: 2 }), h3: e.isActive("heading", { level: 3 }),
      bullet: e.isActive("bulletList"), ordered: e.isActive("orderedList"), quote: e.isActive("blockquote"),
      link: e.isActive("link"), undo: e.can().undo(), redo: e.can().redo(),
    }),
  });

  const setLink = () => {
    const previous = editor.getAttributes("link").href as string | undefined;
    const url = window.prompt("Adresse du lien (https://… ou /page-du-site)", previous ?? "https://");
    if (url === null) return;
    if (url === "") editor.chain().focus().unsetLink().run();
    else editor.chain().focus().extendMarkRange("link").setLink({ href: url }).run();
  };

  const tools: { label: string; Icon: LucideIcon; active?: boolean; disabled?: boolean; run: () => void }[] = [
    { label: "Intertitre", Icon: Heading2Icon, active: state.h2, run: () => editor.chain().focus().toggleHeading({ level: 2 }).run() },
    { label: "Sous-titre", Icon: Heading3Icon, active: state.h3, run: () => editor.chain().focus().toggleHeading({ level: 3 }).run() },
    { label: "Gras", Icon: BoldIcon, active: state.bold, run: () => editor.chain().focus().toggleBold().run() },
    { label: "Italique", Icon: ItalicIcon, active: state.italic, run: () => editor.chain().focus().toggleItalic().run() },
    { label: "Souligné", Icon: UnderlineIcon, active: state.underline, run: () => editor.chain().focus().toggleUnderline().run() },
    { label: "Liste à puces", Icon: ListIcon, active: state.bullet, run: () => editor.chain().focus().toggleBulletList().run() },
    { label: "Liste numérotée", Icon: ListOrderedIcon, active: state.ordered, run: () => editor.chain().focus().toggleOrderedList().run() },
    { label: "Citation", Icon: QuoteIcon, active: state.quote, run: () => editor.chain().focus().toggleBlockquote().run() },
    { label: "Lien", Icon: Link2Icon, active: state.link, run: setLink },
    { label: "Annuler", Icon: Undo2Icon, disabled: !state.undo, run: () => editor.chain().focus().undo().run() },
    { label: "Rétablir", Icon: Redo2Icon, disabled: !state.redo, run: () => editor.chain().focus().redo().run() },
  ];

  return (
    <div className="overflow-hidden rounded-3xl border bg-background focus-within:ring-3 focus-within:ring-ring/40">
      <div role="toolbar" aria-label={`Mise en forme : ${label}`} className="flex flex-wrap gap-1 border-b bg-muted/50 p-2">
        {tools.map(({ label: toolLabel, Icon, active, disabled, run }) => (
          <button
            key={toolLabel}
            type="button"
            title={toolLabel}
            aria-label={toolLabel}
            aria-pressed={active}
            disabled={disabled}
            onClick={run}
            className={cn(
              "inline-flex size-9 items-center justify-center rounded-full text-foreground transition-colors hover:bg-background disabled:opacity-40",
              active && "bg-primary text-primary-foreground hover:bg-primary",
            )}
          >
            <Icon className="size-4" aria-hidden="true" />
          </button>
        ))}
      </div>
      <EditorContent editor={editor} />
    </div>
  );
}
