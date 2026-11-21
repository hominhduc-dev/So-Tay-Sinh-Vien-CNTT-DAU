import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { AdminDashboard } from "@/modules/admin/views/AdminDashboard";

export const metadata: Metadata = { title: "Trạng thái hệ thống" };

export default function HealthPage() {
  return <main className="min-h-screen"><Header section="admin" /><AdminDashboard activeTab="health" /></main>;
}
