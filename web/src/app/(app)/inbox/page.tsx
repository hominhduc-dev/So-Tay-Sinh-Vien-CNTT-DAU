import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { Inbox } from "@/features/inquiries/Inbox";

export const metadata: Metadata = { title: "Hộp thư" };

export default function InboxPage() {
  return <main className="min-h-screen bg-base"><Header section="inbox" /><Inbox /></main>;
}
