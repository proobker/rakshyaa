# Rakshyaa - Women's Safety Android Application

A comprehensive women's safety Android application using Supabase as the backend for storage, authentication, and real-time features.

**Current Version**: 1.1 (Release Candidate)

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for details.

## Features

- **Authentication**: Secure sign-up/login with Supabase Auth
- **Location Tracking**: Foreground service for continuous GPS tracking
- **SOS Emergency System**: 
  - Voice-activated SOS triggering
  - 5-second countdown to prevent false alarms
  - Emergency calling (911) integration
  - Incident reporting and real-time location sharing
- **Encrypted Video Upload**: 
  - Client-side encryption using AES-256-GCM
  - Front/rear camera capture with CameraX
  - Secure upload to Supabase Storage
- **Ride Monitoring**: 
  - GPS logging with route deviation detection (Haversine formula)
  - Deviation alerts when leaving safe zones
- **Safe Places Discovery**: 
  - Geospatial queries for nearby hospitals/police/fire stations
  - User-submitted safe place contributions
- **Check-ins System**: 
  - Scheduled safety check-ins with grace periods
  - Geofence validation and escalation procedures
- **Emergency Contacts Management**: 
  - Encrypted storage of contact information
  - Escalation procedures for missed check-ins
- **Fake Call Feature**: 
  - Realistic incoming call simulation for escape scenarios
- **Legal Help Section**: 
  - Offline access to legal resources and support information

## Project Structure

```
rakshyaa/
├── app/                    # Android application source
│   ├── src/
│   │   └── main/
│   │       ├── java/com/rakshyaa/rakshyaa/  # Kotlin source code
│   │       └── AndroidManifest.xml
├── admin/                  # Admin portal (Vercel-hosted) - Under development as part of Phase 4
│   ├── pages/              # Next.js pages
│   ├── lib/                # Utilities (Supabase client, etc.)
│   ├── styles/             # CSS and styling
│   ├── public/             # Static assets
│   ├── package.json        # npm dependencies and scripts
│   └── tsconfig.json       # TypeScript configuration
├── docs/                   # Implementation documentation
│   ├── FINAL_IMPLEMENTATION_SUMMARY.md
│   ├── IMPLEMENTATION_SUMMARY.md
│   ├── OVERALL_IMPLEMENTATION_SUMMARY.md
│   ├── PHASE_2_SUMMARY.md
│   └── PHASE_3_SUMMARY.md
├── .mcp.json              # Supabase MCP configuration
├── CLAUDE.md              # Claude Code guidance for this repository
├── LOCAL_SETUP.md         # Local setup instructions
├── LICENSE                # MIT License
├── build.gradle           # App-level Gradle configuration
└── settings.gradle        # Project-level Gradle configuration
```

## Documentation

Detailed implementation summaries are available in the `docs/` directory:
- [FINAL_IMPLEMENTATION_SUMMARY.md](docs/FINAL_IMPLEMENTATION_SUMMARY.md) - Complete implementation overview
- [OVERALL_IMPLEMENTATION_SUMMARY.md](docs/OVERALL_IMPLEMENTATION_SUMMARY.md) - Progress by phase
- [PHASE_2_SUMMARY.md](docs/PHASE_2_SUMMARY.md) - Core location & SOS features
- [PHASE_3_SUMMARY.md](docs/PHASE_3_SUMMARY.md) - Media, monitoring & support features
- [IMPLEMENTATION_SUMMARY.md](docs/IMPLEMENTATION_SUMMARY.md) - Initial authentication setup

## Development Setup

### Prerequisites
- Android Studio or equivalent Android development environment
- Node.js (for Supabase CLI and admin portal development)
- Supabase account and project (configured via .mcp.json)
- Git for version control

### Environment Setup
1. Clone the repository
2. Ensure Supabase MCP server is configured (see .mcp.json)
3. Install Android SDK and required build tools
4. Install Node.js dependencies for admin portal: `cd admin && npm install`

### Common Development Commands
- Build the app: Use Android Studio or Gradle wrapper (`./gradlew assembleDebug`)
- Run tests: `./gradlew test`
- Run on emulator/device: `./gradlew installDebug`
- Linting: `./gradlew lint`
- Admin portal development: `cd admin && npm run dev` (starts development server at http://localhost:3000)

## Architecture

### High-Level Architecture
1. **Android App (Client)** - Native Android application with:
   - Foreground service for continuous location tracking
   - Encrypted local storage for sensitive data
   - Camera integration for front/back video capture
   - Voice activation module for hands-free SOS
   - GPS logging and route deviation analysis
   - Geofencing for safe places and check-ins
   - Emergency contact management with encryption
   - Legal resources section

2. **Supabase Backend** - Provides:
   - Authentication (email/password, social login)
   - Database (PostgreSQL) for user profiles, contacts, incidents, etc.
   - Storage for encrypted video uploads
   - Edge Functions for custom logic (SOS processing, route analysis)
   - Real-time subscriptions for live location tracking
   - Security policies (RLS) for data protection

3. **Admin Portal** (Vercel-hosted) - Web interface for:
   - Monitoring active users and their locations
   - Viewing incident reports and SOS alerts
   - Managing safe places database
   - Accessing legal resources and support

## Security Features

- End-to-end encryption for sensitive media (video/audio)
- Supabase Row Level Security (RLS) extensively used
- Encryption keys stored securely using Android Keystore
- Authentication tokens encrypted at rest using AES256-GCM
- All network communications over HTTPS
- Input validation and sanitization to prevent injection attacks
- Data minimization principles applied
- Location sharing only during active SOS or user-enabled tracking

## Getting Started

1. Review this README for feature requirements
2. Set up Android development environment
3. Connect to Supabase project via MCP configuration (.mcp.json)
4. Begin implementing core authentication and user profile features
5. Develop location tracking service with battery optimization
6. Implement SOS triggering mechanism
7. Build encrypted media handling (CameraX + encryption)
8. Add remaining features incrementally
9. For admin portal development: `cd admin && npm run dev`
10. Test thoroughly on various Android versions and devices

## Running the Application

### Development Mode
To run the application in development mode:

1. **Android App**:
   - Ensure you have Android Studio installed and configured
   - Connect an Android device or start an emulator
   - Run: `./gradlew installDebug`
   - Or use Android Studio's "Run" button

2. **Admin Portal** (if applicable):
   ```bash
   cd admin
   npm install  # Install dependencies (first time only)
   npm run dev  # Starts development server at http://localhost:3000
   ```

### Production Build
To create a production-ready build:

1. **Android App**:
   - Ensure you have a valid signing key configured
   - Run: `./gradlew bundleRelease`
   - The output will be at `app/build/outputs/bundle/release/app-release.aab`

2. **Admin Portal** (if applicable):
   ```bash
   cd admin
   npm run build  # Creates production build in .next/
   npm run start  # Starts production server
   ```

## Releasing the Application

Follow the detailed release process in [RELEASE.md](RELEASE.md) for versioning, building, testing, and deploying releases.

Key steps include:
1. Update version code and name in `app/build.gradle`
2. Update release notes in `RELEASE_NOTES.md`
3. Build release bundle with proper signing
4. Test the release build thoroughly
5. Deploy to internal/closed testing (optional)
6. Promote to production in Google Play Console

For detailed instructions, see [RELEASE.md](RELEASE.md).

## License

This project is licensed under the MIT License - see the LICENSE file for details.