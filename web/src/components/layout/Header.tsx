import Image from "next/image";
import Link from "next/link";
import { MainNav, type Section } from "./MainNav";
import { UserMenu } from "./UserMenu";

export function Header({ section }: { section: Section }) {
  return (
    <header className="sticky top-0 z-30 border-b border-border bg-base/95 backdrop-blur-sm">
      <div className="mx-auto flex h-16 max-w-[1184px] items-center gap-6 px-6 sm:px-8">
        <Link href="/chat" className="flex min-w-0 items-center gap-2.5 no-underline">
          <Image src="/logo-dau.png" alt="DAU" width={28} height={28} />
          <span className="truncate font-serif text-[16px] font-semibold text-primary sm:text-[18px]">Sổ Tay Sinh Viên CNTT</span>
        </Link>
        <MainNav active={section} className="hidden sm:flex" />
        <div className="ml-auto"><UserMenu compact /></div>
      </div>
      <MainNav active={section} className="flex overflow-x-auto border-t border-border px-4 py-1.5 sm:hidden" />
    </header>
  );
}
