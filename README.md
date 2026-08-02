<img width="1078" height="909" alt="image" src="https://github.com/user-attachments/assets/8ecb9ed8-b4c0-494a-b026-d8ece33191ae" />

Common problem: Empty Tool Window

If the Call Graph tool window is empty and the generated diagram is not displayed, JCEF may be running in out-of-process mode.

Solution:

1. Open Help → Edit Custom VM Options...
2. Add the following line:
'''
-Dide.browser.jcef.out-of-process.enabled=false
'''
3. Restart IntelliJ IDEA.

After restarting, the Call Graph window should display the generated Mermaid diagram correctly.
