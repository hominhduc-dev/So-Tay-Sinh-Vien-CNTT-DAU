import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { ContentManager } from "@/features/content/ContentManager";

export const metadata: Metadata = { title: "Nội dung" };

export default function ContentPage() {
  return (
    <main className="min-h-screen bg-base">
      <Header section="content" />
      <ContentManager />
    </main>
  );
}
