export default function Footer({ name }: { name: string }) {
  return (
    <footer className="footer">
      <p>
        © {new Date().getFullYear()} {name}. All rights reserved.
      </p>
      <p className="muted">
        🔒 Your phone number is used only to confirm your booking. We never share it.
      </p>
    </footer>
  )
}
