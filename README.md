# Glassh

A glass-styled SSH monitoring client for Android. Connects to your VPS over SSH, then renders a live dashboard of CPU temperature, load, memory, disk and network — wrapped in an iOS 18 inspired liquid-glass UI.

![min API 26](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white) ![License](https://img.shields.io/badge/license-MIT-blue.svg)

## Features

- **Liquid-glass UI** — iridescent gradient backdrop, blurred color blobs, frosted panels with edge highlight and gradient borders. Auto adapts to light and dark.
- **VPS dashboard** — CPU usage ring, real-time temperature ring with per-sensor breakdown, RAM / swap bars, disk usage with color-coded thresholds, network down/up rates derived between samples.
- **Zero-agent collection** — a single SSH `exec` runs a small POSIX shell script that reads `/proc`, `/sys/class/thermal`, `/sys/class/hwmon`, `df`, `/proc/net/dev`. No daemon, no extra packages, works on most Linux VPS images out of the box.
- **Multiple servers** — managed list with edit and delete; data stored in DataStore, JSON serialized.
- **Password and key auth** — full OpenSSH private key paste with optional passphrase. Optional pinned SHA256 host key fingerprint (TOFU when unset).
- **Auto refresh** — pulls every 5 seconds while the dashboard is open.

## Install

Grab the latest debug APK from the [Releases](https://github.com/Minis233/glassh/releases) page or build from source:

```
./gradlew :app:assembleDebug
```

The CI workflow at `.github/workflows/build.yml` produces a `glassh-debug` artifact on every push to `main`. Tags matching `v*` automatically attach signed-by-debug-key APKs to a GitHub Release.

## How temperature is collected

`/sys/class/thermal/thermal_zone*/temp` for CPU package and SoC sensors, `/sys/class/hwmon/hwmonN/temp*_input` (with optional `tempN_label`) for motherboard / NVMe / drive sensors. Values are converted from milli-degrees to °C, filtered to the plausible range (-10°C to 150°C), and the highest reading is shown in the ring with the rest listed below.

If your VPS is virtualized without exposing thermal zones (most KVM / OpenVZ guests do not), the temperature card will say "No sensors detected". CPU usage, memory, disks, and network still work everywhere.

## Architecture

```
ui/            Compose screens + GlassBackdrop / GlassSurface primitives
ssh/           JSch session handling + remote collection script + parser
data/          DataStore-backed HostRepository
model/         Plain Kotlin data classes (HostConfig, VpsStats, DiskInfo)
```

The remote collector emits sectioned output (`===SECTION:cpustat1===` etc.) so the parser can split robustly even when individual probes fail.

## Security notes

- Host keys: not yet stored automatically. Set the SHA256 fingerprint by hand under "Host key" if you want strict pinning. Get the fingerprint with:
  ```
  ssh-keyscan -t ed25519 your.host | ssh-keygen -lf - -E sha256
  ```
  Paste the part after `SHA256:`.
- Passwords and private keys live in DataStore as plain JSON for now. If you store sensitive credentials, consider using key-based auth and treating the device as trusted. Encrypted at rest is on the roadmap.

## Roadmap

- [ ] Interactive shell terminal tab (channel="shell", VT100 renderer)
- [ ] Encrypted credential storage via Android Keystore
- [ ] Historical line charts (sparkline per metric)
- [ ] iOS-style widget on home screen via Glance

## License

MIT — see [LICENSE](LICENSE).
