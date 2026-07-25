import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'InstaAuto DM — Instagram Comment-to-DM SaaS',
  description: 'Automate Instagram Direct Messages from comment triggers using the Meta Graph API.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="antialiased selection:bg-indigo-500 selection:text-white">
        {children}
      </body>
    </html>
  );
}
