import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

# Replace the "Quay lại danh mục" row and Column wrapper
pattern = r"""                } else \{
                    // Sub-screen for the selected section
                    Column\(
                        modifier = Modifier\.fillMaxSize\(\)
                    \) \{
                        Row\(
                            modifier = Modifier\.fillMaxWidth\(\),
                            verticalAlignment = Alignment\.CenterVertically
                        \) \{
                            TextButton\(onClick = \{ selectedSection = null \}\) \{
                                Icon\(imageVector = Icons\.AutoMirrored\.Filled\.ArrowBack, contentDescription = null, tint = NeonBlue\)
                                Spacer\(modifier = Modifier\.width\(4\.dp\)\)
                                Text\("Quay lại danh mục", color = NeonBlue, fontWeight = FontWeight\.Bold\)
                            \}
                        \}
                        Spacer\(modifier = Modifier\.height\(8\.dp\)\)

                        when \(selectedSection\) \{"""

replacement = r"""                }
                
                if (selectedSection != null) {
                    when (selectedSection) {"""

new_content = re.sub(pattern, replacement, content)

# Fix the closing braces for that section
new_content = new_content.replace("""                        }
                    }
                }
            }
        }
    }
}

enum class HubSection""", """                    }
                }
            }
        }
    }
}

enum class HubSection""")

open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(new_content)
