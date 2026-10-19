LUMINARIA APP - ANDROID (APK) BUILD ON GITHUB
=============================================

You do NOT need Android Studio. GitHub builds the APK for you, free.

1. Go to github.com and sign in (free account).
2. Click + (top right) > New repository. Name it  luminaria-app  . Choose Public
   (a Public repo is easiest: anyone with the link can download the APK).
   Click Create repository.
3. Click "uploading an existing file". Extract this zip first, open the
   luminaria-app-android folder, select EVERYTHING inside it and drag it into the page.
   Click Commit changes.
   IMPORTANT: the folder named ".github" must be uploaded too. If it did not upload
   (some computers hide it), click Add file > Create new file, type this exact name:
       .github/workflows/build-apk.yml
   open build-apk.yml from this folder in Notepad, copy everything, paste it, Commit.
4. Click the "Actions" tab. Wait 3-6 minutes for "Build Luminaria App APK" to show a green tick.
   (If it asks, click "I understand my workflows, go ahead and enable them".)
5. Download the APK:
   - Easiest: on the repo's main page, click "Releases" (right side) > Luminaria-App.apk
   - Or: Actions > the finished run > Artifacts > Luminaria-App-apk (a zip containing the APK).
6. Copy Luminaria-App.apk to the phone/tablet (or open the Releases link on the phone), tap it, allow
   "Install unknown apps" for your browser/file manager if Android asks, then Install.
   If Play Protect shows a warning, choose "Install anyway" (the app is signed with a
   debug key, not published on the Play Store).

TO UPDATE LATER: replace app/src/main/assets/index.html with the new Luminaria App file on GitHub
(open the file > pencil icon, or Add file > Upload files). A new APK is built automatically.

WHAT THE APK DOES
- Opens the Luminaria App full screen, works fully offline, no address bar.
- Backup / result-picture buttons save into the phone's Downloads folder.
- "Restore from file" opens the phone's file picker.
- Icon: the blue Luminaria logo filling the whole square.
