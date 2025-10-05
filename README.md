# Authenticator

Open source authenticator app for Android.

Built using **Kotlin** and **Jetpack Compose** — Android's modern toolkit for
building native UIs. This app demonstrates best practices with a clean architecture, modern
libraries, and Compose UI components.

## Features

- TOTP and HOTP support
- Import from Google Authenticator
- Material You design

## Download

- Download the latest APK from the [Releases](https://github.com/mcbabo/Authenticator/releases/latest) page.

## Contributing

Contributions are welcome!

## Star History

<a href="https://www.star-history.com/#mcbabo/Authenticator&Timeline">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=mcbabo/Authenticator&type=Timeline&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=mcbabo/Authenticator&type=Timeline" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=mcbabo/Authenticator&type=Timeline" />
 </picture>
</a>

## Project Structure

```
at.mcbabo.authenticator/
├── data                # Data layer
│     ├── repository
│     ├── db
│     ├── di
│     └── store
├── internal            # Internal utilities and helpers
│     └── crypto
├── navigation          # Navigation components
├── ui                  # UI layer
│     ├── component
│     ├── screen
│     ├── theme
│     └── viewmodel
└── MainActivity.kt     # Entry point
```

## Author

**mcbabo**  
[GitHub](https://github.com/mcbabo)

## Credits

Huge shoutout to the [Seal](https://github.com/seal) team!

The app is mostly inspired by [Seal](https://github.com/seal) and other Material 3 apps

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.