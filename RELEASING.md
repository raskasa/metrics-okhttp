Release Process
===============

 1. Update `CHANGELOG.md` with release date, new features, fixes, and next
 release number.
 
 2. Update `README.md` with new usage and download info.
 
 3. Commit: `git commit -am "Update project documentation for X.Y.Z."`
 
 4. Prepare: `mvn release:prepare release:perform`.  If you encounter the
 following issue:

    ```
    gpg: signing failed: Inappropriate ioctl for device
    ```
    
    ... execute the following before Step 4:
    
    ```
    $ export GPG_TTY=$(tty)
    ```

  5. Verify and release the new artifacts in the Nexus Repository Manager.
