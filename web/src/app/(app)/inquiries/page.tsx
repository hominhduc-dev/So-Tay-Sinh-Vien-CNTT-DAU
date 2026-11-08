import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { MyInquiries } from "@/features/inquiries/MyInquiries";

export const metadata: Metadata = { title: "Câu hỏi đã gửi" };

export default function InquiriesPage() {
  return <main className="min-h-screen bg-base"><Header section="inquiries" /><MyInquiries /></main>;
}
