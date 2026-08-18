# rakshyaa
Women's Safety Android Application

## Overview
This is a women's safety Android application that uses Supabase as the backend for storage, authentication, and real-time features. The app includes features like location tracking, SOS alerts, encrypted video uploads, ride monitoring, safe places discovery, check-ins, and more.

## Getting Started

### Prerequisites
- Android Studio or equivalent Android development environment
- Node.js (for Supabase CLI and potential web components)
- Supabase account and project (configured via .mcp.json)
- Git for version control

### Setup Instructions
1. Clone the repository
2. Set up Supabase credentials (see [LOCAL_SETUP.md](LOCAL_SETUP.md))
3. Ensure Supabase MCP server is configured (see .mcp.json)
4. Install Android SDK and required build tools
5. For any web/admin components, install Node.js dependencies

### Development Setup
Refer to [CLAUDE.md](CLAUDE.md) for detailed development setup, common commands, code architecture, and best practices.

## Features
- Woman's safety focused application
- Supabase integration for storage, login, and auth
- Foreground service for location tracking
- Admin portal accessible via vercel
- Encrypted video upload (front and back camera)
- Smart SOS-calls to 911 with location sharing to admin portal
- SOS activation
- End-to-end encryption for video uploads
- Ride monitoring with GPS logging and deviation evaluation
- Safe places discovery (hospitals, police, fire stations)
- Check-ins with grace periods and geofence validation
- Fake call feature for escape situations
- Encrypted emergency contacts management
- In-app legal help and resources

## Next Steps
See [CLAUDE.md](CLAUDE.md) for detailed development guidance and implementation approach.