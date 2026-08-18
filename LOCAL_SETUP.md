# Local Setup Instructions

## Setting up Supabase Credentials

For security reasons, Supabase credentials should not be hardcoded in the source code. Instead, they should be stored in a `local.properties` file in the project root.

### Step 1: Create local.properties file

Create a file named `local.properties` in the root of the `rakshyaa` module (same level as `build.gradle`).

### Step 2: Add your Supabase credentials

Add the following lines to `local.properties`:

```
SUPABASE_URL=your-supabase-project-url
SUPABASE_ANON_KEY=your-supabase-anon-key
```

Replace:
- `your-supabase-project-url` with your actual Supabase project URL (e.g., `https://your-project-ref.supabase.co`)
- `your-supabase-anon-key` with your actual Supabase anon key

### Step 3: Ensure local.properties is gitignored

The `local.properties` file should already be gitignored by default in Android projects. If not, add this line to your `.gitignore` file:

```
local.properties
```

### Step 4: Retrieve your Supabase credentials

1. Go to [Supabase](https://supabase.com/) and log in to your account
2. Select your project (referenced in .mcp.json as `glbaaslnwmodgpxqiuwn`)
3. Go to Settings → API
4. Copy the Project URL and anon public key

### Security Note

Never commit the `local.properties` file to version control as it contains sensitive credentials.