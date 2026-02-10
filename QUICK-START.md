# ⚡ Quick Start Guide

Get your multiplayer retrospective game running in 15 minutes!

---

## 🎯 What You'll Get

✅ Real-time multiplayer retrospective game  
✅ Everyone uses their own device  
✅ Instant synchronization  
✅ Free hosting on GitHub Pages  

---

## 📋 Prerequisites

- GitHub account
- Web browser
- 15 minutes

---

## 🚀 Setup (Do Once)

### 1. Create Firebase Project (5 minutes)

1. Go to https://console.firebase.google.com/
2. Click **"Add project"**
3. Name: `retro-retrospective`
4. **Disable** Google Analytics
5. Click **"Create project"**

### 2. Enable Database (2 minutes)

1. Click **"Build"** → **"Realtime Database"**
2. Click **"Create Database"**
3. Choose location (closest to you)
4. Select **"Start in test mode"**
5. Click **"Enable"**

### 3. Get Your Config (1 minute)

1. Click the **web icon** (`</>`) on project homepage
2. App nickname: `Retro Game`
3. **Copy** the `firebaseConfig` object (looks like this):

```javascript
const firebaseConfig = {
  apiKey: "AIza...",
  authDomain: "your-project.firebaseapp.com",
  databaseURL: "https://your-project-default-rtdb.firebaseio.com",
  projectId: "your-project",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
};
```

### 4. Update HTML File (2 minutes)

1. Open `retro-multiplayer.html` in your editor
2. Find line ~450 (search for "YOUR_API_KEY_HERE")
3. **Replace** the placeholder config with YOUR config
4. **Save** the file

### 5. Set Security Rules (2 minutes)

1. In Firebase Console: **Database** → **Rules** tab
2. **Replace** with this:

```json
{
  "rules": {
    "sessions": {
      "$sessionCode": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

3. Click **"Publish"**

### 6. Push to GitHub (3 minutes)

```bash
git add .
git commit -m "Add Firebase config"
git push
```

Then enable GitHub Pages:
1. Go to repo **Settings** → **Pages**
2. Source: **main** branch
3. Click **Save**
4. Wait 1-2 minutes

### 7. Get Your URL

```
https://YOUR-USERNAME.github.io/YOUR-REPO/retro-multiplayer.html
```

---

## 🎮 Use It (Every Retrospective)

### Host:
1. Open the URL
2. Click **"HOST SESSION"**
3. Enter name + session code (e.g., "RETRO2024")
4. Share URL + code with team
5. Wait for team to join (you'll see them!)
6. Select a game
7. Click **"START GAME"**

### Team Members:
1. Open the URL (from host)
2. Click **"JOIN SESSION"**
3. Enter name + session code
4. Wait for host to select game (appears automatically!)
5. Click **"START GAME"**
6. Complete retrospective
7. Download responses

---

## ✅ Test It

1. Open URL on your computer
2. Create session: "TEST123"
3. Open URL on your phone
4. Join session: "TEST123"
5. See yourself appear on both devices! ✨

---

## 🆘 Troubleshooting

### "Firebase not initialized"
→ Check that you replaced ALL config values (no "YOUR_" text)

### "Session not found"
→ Make sure session code matches exactly (case-sensitive)

### "Permission denied"
→ Check Firebase database rules are published

---

## 📚 More Help

- **Detailed setup:** [FIREBASE-SETUP-GUIDE.md](FIREBASE-SETUP-GUIDE.md)
- **Usage guide:** [MULTIPLAYER-SETUP.md](MULTIPLAYER-SETUP.md)
- **What changed:** [FIREBASE-CONVERSION-SUMMARY.md](FIREBASE-CONVERSION-SUMMARY.md)

---

## 🎉 Done!

You now have a real-time multiplayer retrospective game!

**Share your URL with your team and start retrospecting! 🕹️**
