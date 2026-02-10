# 🕹️ Multiplayer Retro Retrospective - Setup Guide

## 🎯 Overview

This guide explains how to set up the multiplayer retrospective game for your team using **Firebase Realtime Database** for true multi-device collaboration.

## ✨ What's New: Firebase Integration

**The game now uses Firebase for real-time synchronization!**

✅ **Real-time sync** - All players see updates instantly  
✅ **Multi-device support** - Everyone uses their own device  
✅ **No manual file sharing** - Everything syncs automatically  
✅ **Free for small teams** - Up to 100 simultaneous connections  

---

## 🚀 Quick Start

### 1. Set Up Firebase (One-Time Setup)

**📖 Follow the detailed guide:** [FIREBASE-SETUP-GUIDE.md](FIREBASE-SETUP-GUIDE.md)

**Quick summary:**
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com/)
2. Enable Realtime Database
3. Copy your Firebase config
4. Replace the config in `retro-multiplayer.html`
5. Push to GitHub Pages

**Time required:** 10-15 minutes (one-time setup)

---

### 2. Deploy to GitHub Pages

1. **Push your files to GitHub:**
   ```bash
   git add retro-multiplayer.html retro-retrospective-game.html
   git commit -m "Add Firebase config"
   git push
   ```

2. **Enable GitHub Pages:**
   - Go to repository Settings → Pages
   - Select main branch
   - Save

3. **Get your URL:**
   ```
   https://YOUR-USERNAME.github.io/YOUR-REPO/retro-multiplayer.html
   ```

---

## 📱 How to Use (For Your Team)

### For the Host:

1. **Open the GitHub Pages URL**
2. **Click "HOST SESSION"**
3. **Enter your name** and create a **session code** (e.g., "RETRO2024")
4. **Share the URL and session code** with your team (via Slack, email, etc.)
5. **Wait for team members to join** - you'll see them appear in the lobby
6. **Select a game** from the 4 options:
   - 🟡 PAC-RETRO
   - 🍄 SUPER SPRINT
   - 👾 SPACE RETRO
   - 🟦 TETRO-SPECT
7. **Click "START GAME"** when ready
8. **Everyone plays** on their own device

### For Team Members:

1. **Open the URL** shared by the host
2. **Click "JOIN SESSION"**
3. **Enter your name** and the **session code**
4. **Wait in the lobby** - you'll see the host select a game
5. **The game appears automatically** when the host selects it
6. **Click "START GAME"** to begin playing
7. **Complete your retrospective** independently

---

## 🎮 Game Features

- **4 Retro Game Themes**: PAC-RETRO, SUPER SPRINT, SPACE RETRO, TETRO-SPECT
- **6 Levels per Game**: 5 regular levels + 1 Iteration Star rating level
- **Real-Time Updates**: See players join instantly, game selection syncs automatically
- **Host Controls**: Only the host can select and start games
- **Individual Responses**: Everyone completes their own retrospective privately
- **Download Summary**: Each person can download their responses at the end

---

## 🔄 Real-Time Features

With Firebase, you get:

### Instant Player Updates
- See team members join the lobby in real-time
- Player list updates automatically
- No refresh needed

### Automatic Game Selection
- Host selects a game
- Everyone sees it instantly
- No file sharing required

### Session Persistence
- Close your browser and come back
- Your session is still there
- Rejoin automatically

---

## 💾 Export/Import (Backup Only)

The export/import buttons are now for **backup purposes only**:

- **Export**: Creates a backup JSON file of your session
- **Import**: Restores a session from backup

You don't need these for normal operation - Firebase handles everything!

---

## 🎯 Recommended Workflow

### Before the Meeting:
1. ✅ Firebase is set up (one-time)
2. ✅ Files are on GitHub Pages
3. ✅ You have the URL ready

### During the Meeting:
```
1. Host creates session with memorable code
2. Host shares URL + session code in meeting chat
3. Everyone joins from their own device
4. Host sees everyone in the lobby
5. Host selects game (everyone sees it instantly)
6. Host starts game
7. Everyone plays independently (15-20 minutes)
8. Everyone downloads their responses
9. Host collects and discusses
```

**Total time:** 30-45 minutes

---

## 📝 Tips for Success

### Session Codes
- Use memorable codes: "SPRINT42", "RETRO2024", "TEAM-JAN"
- Share in meeting chat for easy copy/paste
- Case-sensitive, so communicate clearly

### Communication
- Use video call (Zoom/Teams) during retrospective
- Host announces when game is selected
- Set a time limit (15-20 minutes)
- Discuss responses together after

### Response Collection
- Everyone downloads their own `.txt` file
- Share via Slack/email/Teams
- Host compiles into master document
- Review together as a team

---

## 🔧 Troubleshooting

### "Firebase not initialized" error
- Check that you replaced the Firebase config in `retro-multiplayer.html`
- Verify all config values are correct (no "YOUR_" placeholders)
- See [FIREBASE-SETUP-GUIDE.md](FIREBASE-SETUP-GUIDE.md) for details

### "Session not found"
- Verify the session code is correct (case-sensitive)
- Make sure the host created the session first
- Check that Firebase database rules allow reading

### Players not seeing updates
- Verify everyone is using the same session code
- Check that Firebase is properly configured
- Try refreshing the page
- Check browser console (F12) for errors

### "Permission denied" error
- Check Firebase database rules
- Make sure rules allow read/write access
- See security rules section in [FIREBASE-SETUP-GUIDE.md](FIREBASE-SETUP-GUIDE.md)

---

## 🔒 Security & Privacy

### Current Setup
- Anyone with the session code can join
- Responses are stored in Firebase
- Data is accessible to anyone with database access

### For Production Use
- Add Firebase Authentication
- Restrict database access to authenticated users
- See [FIREBASE-SETUP-GUIDE.md](FIREBASE-SETUP-GUIDE.md) for security rules

### Privacy Notes
- Each person's responses stay on their device until they download
- Only session metadata (players, game selection) is in Firebase
- Individual retrospective answers are NOT synced to Firebase
- Download your responses to share them

---

## 💰 Cost

### Firebase Free Tier
- ✅ 1 GB stored data
- ✅ 10 GB/month downloaded
- ✅ 100 simultaneous connections
- ✅ Perfect for teams up to 100 people

### GitHub Pages
- ✅ Completely free
- ✅ Unlimited bandwidth
- ✅ Custom domains supported

**Total cost: $0** for most teams!

---

## 📊 Monitoring

### Check Your Firebase Usage
1. Go to Firebase Console
2. Click "Realtime Database"
3. View "Usage" tab
4. Monitor connections and bandwidth

### Clean Up Old Sessions
1. Go to Firebase Console → Realtime Database → Data
2. Find old sessions under `/sessions/`
3. Delete manually to free up space

---

## 🆘 Need Help?

### Quick Fixes
1. **Clear browser cache** and try again
2. **Check browser console** (F12) for error messages
3. **Verify Firebase config** is correct
4. **Try a different browser** (Chrome, Firefox, Edge)

### Documentation
- [Firebase Setup Guide](FIREBASE-SETUP-GUIDE.md) - Detailed Firebase instructions
- [Firebase Docs](https://firebase.google.com/docs/database) - Official documentation
- [GitHub Pages Docs](https://docs.github.com/en/pages) - Hosting guide

---

## ✅ Pre-Meeting Checklist

### One-Time Setup (Done Once)
- [ ] Firebase project created
- [ ] Realtime Database enabled
- [ ] Firebase config added to HTML
- [ ] Files pushed to GitHub
- [ ] GitHub Pages enabled
- [ ] URL tested and working

### Before Each Retrospective
- [ ] URL is accessible
- [ ] Session code decided
- [ ] Team has URL and code
- [ ] Video call is ready
- [ ] Time limit communicated (15-20 min)

### During Retrospective
- [ ] Host creates session
- [ ] All team members join
- [ ] Host selects game
- [ ] Everyone sees game selection
- [ ] Everyone completes retrospective
- [ ] Everyone downloads responses

### After Retrospective
- [ ] All response files collected
- [ ] Responses compiled and reviewed
- [ ] Action items identified
- [ ] Follow-up scheduled

---

## 🎮 Example Session Flow

```
TIME  | HOST (Alice)              | MEMBER (Bob)              | MEMBER (Charlie)
------|---------------------------|---------------------------|---------------------------
10:00 | Opens URL                 | -                         | -
10:01 | Creates session "RETRO42" | -                         | -
10:02 | Shares URL + code         | Receives link             | Receives link
10:03 | Waits in lobby            | Opens URL                 | Opens URL
10:04 | Sees Bob join ✅          | Joins session             | -
10:05 | Sees Charlie join ✅      | Waits in lobby            | Joins session
10:06 | Selects 🟡 PAC-RETRO      | Sees game instantly ✅    | Sees game instantly ✅
10:07 | Clicks START GAME         | Clicks START GAME         | Clicks START GAME
10:08 | Plays PAC-RETRO           | Plays PAC-RETRO           | Plays PAC-RETRO
10:20 | Downloads responses       | Downloads responses       | Downloads responses
10:21 | Collects files            | Shares file               | Shares file
10:25 | Compiles summary          | -                         | -
10:30 | Discusses as team         | Discusses as team         | Discusses as team
```

**Total time:** 30 minutes

---

## 🌟 Benefits Over Old System

| Feature | Old (localStorage) | New (Firebase) |
|---------|-------------------|----------------|
| Multi-device | ❌ Manual file sharing | ✅ Automatic sync |
| Real-time updates | ❌ No | ✅ Yes |
| Player list | ❌ Static | ✅ Live updates |
| Game selection | ❌ Manual export/import | ✅ Instant sync |
| Setup complexity | ⚠️ Medium | ⚠️ Medium (one-time) |
| Ongoing effort | ❌ High (every session) | ✅ Low (automatic) |

---

## 🚀 You're Ready!

With Firebase set up, your team can now:
- ✅ Join from any device
- ✅ See updates in real-time
- ✅ No manual file sharing
- ✅ Seamless collaboration

**Happy Retrospecting! 🎮👾🕹️**
