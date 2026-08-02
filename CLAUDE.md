# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a women's safety Android application called "rakshyaa" that uses Supabase as the backend for storage, authentication, and real-time features. The app includes features like location tracking, SOS alerts, encrypted video uploads, ride monitoring, safe places discovery, check-ins, and more.

## Development Setup

### Prerequisites
- Android Studio or equivalent Android development environment
- Node.js (for Supabase CLI and potential web components)
- Supabase account and project (configured via .mcp.json)
- Git for version control

### Environment Setup
1. Clone the repository
2. Ensure Supabase MCP server is configured (see .mcp.json)
3. Install Android SDK and required build tools
4. For any web/admin components, install Node.js dependencies

## Common Development Commands

### Supabase Integration
Since this project uses Supabase as the primary backend:
- Supabase MCP server is configured at: https://mcp.supabase.com/mcp?project_ref=glbaaslnwmodgpxqiuwn&features=docs%2Caccount%2Cdatabase%2Cdebugging%2Cdevelopment%2Cfunctions%2Cbranching
- Use Supabase for authentication, database, storage, and edge functions
- Refer to Supabase documentation for schema design, migrations, and security

### Android Development
- Build the app: Use Android Studio or Gradle wrapper (`./gradlew assembleDebug`)
- Run tests: `./gradlew test`
- Run on emulator/device: `./gradlew installDebug`
- Linting: `./gradlew lint`

### General Commands
- Initialize git repository: `git init`
- Commit changes: `git add . && git commit -m "message"`
- Check status: `git status`
- View logs: `adb logcat` (for Android debugging)

## Code Architecture & Structure

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

### Key Features Implementation
- **Location Tracking**: Foreground service with periodic GPS updates to Supabase
- **SOS System**: Voice-activated trigger, smart calling to 911 + admin notification
- **Video Upload**: Client-side encryption before upload to Supabase Storage
- **Ride Monitoring**: GPS logging with route deviation calculation using Haversine formula
- **Safe Places**: Geospatial queries against hospitals/police/fire stations dataset
- **Check-ins**: Scheduled tasks with geofence validation and grace periods
- **Fake Call**: Simulated incoming call interface for decoy scenarios
- **Contacts**: Encrypted storage of emergency contact information with public keys

### Data Models (Supabase Schema)
- `users`: Authentication and profile information
- `emergency_contacts`: Contact details with optional encryption keys
- `location_logs`: Timestamped GPS coordinates for tracking
- `incidents`: SOS events with location, media, and status
- `safe_places`: Predefined locations of hospitals, police stations, etc.
- `check_ins`: Scheduled safety check-ins with response tracking
- `ride_sessions`: Active monitoring sessions with route data

## Best Practices

### Security
- Implement end-to-end encryption for sensitive media (video/audio)
- Use Supabase Row Level Security (RLS) extensively
- Store encryption keys securely using Android Keystore
- Validate and sanitize all inputs to prevent injection attacks
- Use HTTPS for all network communications

### Performance
- Optimize foreground service to minimize battery drain
- Use efficient geoqueries with proper indexing in PostGIS
- Implement caching for frequently accessed safe places data
- Compress and chunk video uploads for better reliability
- Use WorkManager for background tasks where appropriate

### Testing
- Write unit tests for business logic and encryption utilities
- Instrumentation tests for Android components (services, receivers)
- Test edge cases like network loss, GPS unavailability, low battery
- Verify encryption/decryption workflows thoroughly

## Working with Supabase

### Schema Management
- Use Supabase migrations for schema changes
- Follow naming conventions: snake_case for tables and columns
- Enable Row Level Security on all tables by default
- Create appropriate indexes for geoqueries and frequent lookups

### Authentication
- Implement email/password and social login providers
- Use Supabase Auth UI or custom implementation
- Handle token refresh and session management properly
- Link authentication with user profiles in database

### Storage
- Store encrypted videos in Supabase Storage buckets
- Implement client-side encryption before upload
- Use signed URLs for secure, time-limited access
- Consider implementing video transcoding for different qualities

### Edge Functions
- Deploy custom logic for SOS processing, location analysis
- Use for webhooks to external services (emergency services)
- Implement rate limiting and input validation
- Log function executions for monitoring and debugging

## Getting Started

1. Review the README.md for feature requirements
2. Set up Android development environment
3. Connect to Supabase project via MCP configuration
4. Begin implementing core authentication and user profile features
5. Develop location tracking service with battery optimization
6. Implement SOS triggering mechanism
7. Build encrypted media handling
8. Create admin portal interface (ifweb-based)
9. Add remaining features incrementally
10. Test thoroughly on various Android versions and devices

## Troubleshooting

- Check Android Studio logs for build/runtime issues
- Use Supabase dashboard to monitor database queries and auth events
- Verify network connectivity and API endpoints
- Check encryption keys and Android Keystore availability
- Monitor foreground service battery usage via Android Profiler