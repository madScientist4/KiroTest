# 🎮 Multi-Device Retrospective Setup Guide

## Overview

This guide explains how to run a collaborative retrospective where each team member uses their own device.

## 📋 Prerequisites

- All team members need a web browser
- Access to the HTML files (`retro-multiplayer.html` and `retro-retrospective-game.html`)
- A way to share files (email, Slack, Teams, shared drive, etc.)

---

## 🚀 Setup Methods

### Method 1: Shared Web Hosting (Recommended)

**Best for:** Teams with access to a web server or cloud storage

1. **Host uploads both files to a web server:**
   - Upload `retro-multiplayer.html`
   - Upload `retro-retrospective-game.html`
   - Get the URL (e.g., `https://yourserver.com/retro-multiplayer.html`)

2. **Host shares the URL with team:**
   - Send link via email/Slack/Teams
   - Include the session code

3. **Everyone opens the URL:**
   - Host creates session with code (e.g., "RETRO2024")
   - Team members join with the same code
   - Host selects game and starts

**Pros:** ✅ Real-time, easy to use, no file sharing needed  
**Cons:** ❌ Requires web hosting

---

### Method 2: Local Files + Session Export/Import

**Best for:** Teams without web hosting

#### Step 1: Initial Setup (Host)

1. **Host opens `retro-multiplayer.html` locally**
   - Double-click the file or open in browser
   
2. **Host creates a session:**
   - Click "HOST SESSION"
   - Enter name: "Alice"
   - Enter session code: "RETRO2024"
   - Click "CREATE SESSION"

3. **Host exports the session:**
   - Click "📤 EXPORT" button
   - Save the JSON file: `retro-session-RETRO2024.json`

4. **Host shares the session file:**
   - Email the JSON file to all team members
   - Or upload to Slack/Teams/shared drive

#### Step 2: Team Members Join

1. **Each team member opens `retro-multiplayer.html` locally**

2. **Import the session:**
   - Click "JOIN SESSION"
   - Click "📥 IMPORT" button
   - Select the `retro-session-RETRO2024.json` file
   - Go back to setup screen

3. **Join the session:**
   - Click "JOIN SESSION" again
   - Enter their name: "Bob", "Charlie", etc.
   - Enter session code: "RETRO2024"
   - Click "JOIN SESSION"

#### Step 3: Game Selection (Host Only)

1. **Host selects the game:**
   - Choose one of the 4 game modes:
     - 🟡 PAC-RETRO
     - 🍄 SUPER SPRINT
     - 👾 SPACE RETRO
     - 🟦 TETRO-SPECT

2. **Host exports updated session:**
   - Click "📤 EXPORT" again
   - Share the updated JSON file

3. **Team members re-import:**
   - Click "📥 IMPORT"
   - Select the new JSON file
   - They'll see the selected game

#### Step 4: Play the Game

1. **Host starts the game:**
   - Click "START GAME"

2. **Team members start on their devices:**
   - Each person opens `retro-retrospective-game.html` directly
   - Manually select the same game the host chose
   - Everyone plays independently

#### Step 5: Collect Responses

1. **Everyone completes their retrospective:**
   - Answer all questions
   - Rate the iteration (⭐ ITERATION STAR level)

2. **Everyone downloads their responses:**
   - Click "💾 SAVE GAME" at the end
   - Save their individual `.txt` file

3. **Host collects all files:**
   - Team members email/share their `.txt` files
   - Host compiles all responses into one document

**Pros:** ✅ Works without web hosting, everyone uses own device  
**Cons:** ❌ Manual file sharing, not real-time

---

### Method 3: Simple File Sharing (Easiest)

**Best for:** Small teams, quick retrospectives

1. **Host shares both HTML files:**
   - Send `retro-multiplayer.html` and `retro-retrospective-game.html` to everyone
   - Or put them in a shared folder (Dropbox, Google Drive, etc.)

2. **Everyone opens `retro-retrospective-game.html` directly:**
   - Skip the multiplayer lobby entirely
   - Each person chooses the same game (agreed beforehand)

3. **Everyone plays independently:**
   - Answer all questions on their own device
   - Download their responses at the end

4. **Host collects responses:**
   - Everyone shares their `.txt` files
   - Host compiles into summary

**Pros:** ✅ Simplest setup, no session management  
**Cons:** ❌ No coordination, manual compilation

---

## 🎯 Recommended Workflow

### For Teams with Web Hosting:
```
1. Host uploads files to web server
2. Share URL + session code
3. Everyone joins via browser
4. Host selects game
5. Everyone plays
6. Host exports final summary
```

### For Teams without Web Hosting:
```
1. Host creates session locally
2. Export and share JSON file
3. Team imports and joins
4. Host selects game and exports again
5. Team re-imports to see game selection
6. Everyone opens game directly
7. Everyone plays same game
8. Share individual response files
9. Host compiles all responses
```

---

## 📝 Tips for Success

### Communication
- Use video call (Zoom/Teams) during retrospective
- Host announces which game to play
- Set a time limit for completion

### File Naming
- Use clear names: `retro-alice-responses.txt`
- Include date: `retro-2024-02-10-bob.txt`
- Keep session code consistent

### Session Codes
- Use memorable codes: "SPRINT42", "RETRO2024"
- Share via chat/email
- Write it down for reference

### Compilation
- Host creates master document
- Copy/paste each person's responses
- Organize by question/level
- Share final summary with team

---

## 🔧 Troubleshooting

### "Session not found"
- Make sure you imported the latest JSON file
- Check that session code matches exactly (case-sensitive)
- Try exporting and re-importing

### "Can't see selected game"
- Host needs to export after selecting game
- Team members need to re-import the updated file
- Refresh the page after importing

### "Responses not syncing"
- This system doesn't sync in real-time
- Each person's responses stay on their device
- Use export/import to share session state
- Use individual downloads to share responses

---

## 🌐 Future: Real-Time Collaboration

For true real-time multi-device collaboration, you would need:

1. **Backend Server** (Node.js, Python, etc.)
2. **Database** (Firebase, MongoDB, PostgreSQL)
3. **WebSocket Server** (Socket.io, WebSockets)
4. **Hosting** (Heroku, AWS, Azure, Vercel)

This would enable:
- ✅ Real-time synchronization
- ✅ Live player updates
- ✅ Instant game selection sync
- ✅ Automatic response collection
- ✅ No file sharing needed

---

## 📚 Quick Reference

| Action | Host | Team Member |
|--------|------|-------------|
| Create Session | ✅ Yes | ❌ No |
| Join Session | ✅ Yes | ✅ Yes |
| Select Game | ✅ Yes | ❌ No |
| Start Game | ✅ Yes | ❌ No |
| Play Game | ✅ Yes | ✅ Yes |
| Export Session | ✅ Yes | ⚠️ Optional |
| Import Session | ✅ Yes | ✅ Yes |
| Download Responses | ✅ Yes | ✅ Yes |

---

## 🎮 Example Session Flow

```
TIME  | HOST (Alice)              | MEMBER (Bob)              | MEMBER (Charlie)
------|---------------------------|---------------------------|---------------------------
10:00 | Creates session "RETRO42" | -                         | -
10:01 | Exports session JSON      | -                         | -
10:02 | Shares file via Slack     | Receives file             | Receives file
10:03 | -                         | Imports JSON              | Imports JSON
10:04 | -                         | Joins session             | Joins session
10:05 | Selects 🟡 PAC-RETRO      | -                         | -
10:06 | Exports updated JSON      | -                         | -
10:07 | Shares updated file       | Imports update            | Imports update
10:08 | Starts game               | Opens game manually       | Opens game manually
10:09 | Plays PAC-RETRO           | Plays PAC-RETRO           | Plays PAC-RETRO
10:20 | Downloads responses       | Downloads responses       | Downloads responses
10:21 | Collects all files        | Shares file with Alice    | Shares file with Alice
10:25 | Compiles final summary    | -                         | -
10:30 | Shares summary with team  | Reviews summary           | Reviews summary
```

---

## ✅ Checklist

### Before the Retrospective
- [ ] All team members have the HTML files
- [ ] Host has created a session code
- [ ] Communication channel is set up (Zoom/Teams)
- [ ] File sharing method is ready (email/Slack)

### During the Retrospective
- [ ] Host creates and exports session
- [ ] Team imports and joins
- [ ] Host selects game and exports
- [ ] Team re-imports to see game
- [ ] Everyone plays the same game
- [ ] Everyone downloads their responses

### After the Retrospective
- [ ] All response files collected
- [ ] Host compiles summary
- [ ] Summary shared with team
- [ ] Action items identified
- [ ] Follow-up scheduled

---

## 🆘 Need Help?

If you encounter issues:
1. Check browser console for errors (F12)
2. Verify all files are in the same folder
3. Try a different browser
4. Clear browser cache and localStorage
5. Start with a fresh session

---

**Happy Retrospecting! 🎮✨**
