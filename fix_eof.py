content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

import re

# Remove any extra closing brackets at the end of the file
while True:
    if content.strip().endswith("}"):
        content_test = content[:content.rindex("}")]
        open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(content_test)
        
        # Test compile
        import subprocess
        result = subprocess.run(["gradle", "compileDebugKotlin"], capture_output=True, text=True)
        if "Syntax error: Expecting a top level declaration" in result.stderr or "Syntax error: Expecting a top level declaration" in result.stdout:
            print("Still expecting top level decl. Removing another brace...")
            content = content_test
        elif "Expecting '}'" in result.stderr or "Expecting '}'" in result.stdout:
            print("Removed too many! Restoring one...")
            open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(content_test + "}\n")
            break
        elif "Build failed" in result.stderr or "Build failed" in result.stdout:
            print("Other error. Check it:")
            print(result.stdout)
            print(result.stderr)
            break
        else:
            print("Success!")
            break
    else:
        break
