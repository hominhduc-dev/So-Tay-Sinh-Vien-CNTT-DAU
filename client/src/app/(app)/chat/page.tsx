import type { Metadata } from "next";
import { ChatBox } from "@/modules/chat/views/ChatBox";

export const metadata: Metadata = { title: "Hỏi đáp" };

export default function ChatPage() {
  return <ChatBox />;
}
