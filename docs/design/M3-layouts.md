# M3 layouts — described before implementation

Account screen: title and ownership explanation; one full-width Sign in with Microsoft button; current authentication stage with cancellation; saved account cards with a colored initial, Java name, selected state, previous verification status, Select and Remove controls. No password fields. Loading, signed-out, offline, recoverable error, missing configuration, approval pending and storage failure states use readable text and a retry or setup action. Destructive local reset/removal has confirmation.

Account menu from Home and an Accounts action in Settings open this screen. A native callback Activity validates the registered URI and hands it to the same repository without logging its code or exposing credentials to another app component. First-run text is updated to describe the implemented sign-in.

Approval message: "Vojo is waiting for Mojang approval. Minecraft Services has not approved this app registration yet. Your Microsoft account may be fine. Please try again after Vojo's developer receives approval."

Dark and light Compose previews cover signed out, saved accounts, loading, approval pending, offline and errors. Content scrolls, text scales, controls are at least 48dp, and selected/error status is conveyed in text. All physical device tests: **test later**.
