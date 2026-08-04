import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

pattern = r"""                                testTag = "                    \}
                \}

                when \(selectedSection\) \{
                    HubSection\.COMPANY_INFO -> CompanyInfoSection\(defaultConfig, viewModel\) \{ selectedSection = null \}
                    HubSection\.SHIFTS -> ShiftManagementDialogContent\(viewModel\.shiftRepository, companyId\) \{ selectedSection = null \}
                    HubSection\.WORK_RULES -> WorkRuleManagementDialogContent\(viewModel\.workRuleRepository, companyId\) \{ selectedSection = null \}
                    HubSection\.OVERTIME_RULES -> OvertimeRuleManagementDialogContent\(viewModel\.overtimeRuleRepository, companyId\) \{ selectedSection = null \}
                    HubSection\.HOLIDAYS -> HolidaysSection\(companyId\) \{ selectedSection = null \}
                    HubSection\.ALLOWANCES -> AllowancesSection\(defaultConfig, viewModel\) \{ selectedSection = null \}
                    HubSection\.ADVANCED -> AdvancedSection\(defaultConfig, viewModel\) \{ selectedSection = null \}
                    null -> \{\}
                \}
            \}
        \}
    \}
\}defaultConfig, viewModel\) \{ selectedSection = null \}
                            null -> \{\}
                    \}
                \}
            \}
        \}
    \}
\}"""

replacement = r"""                                testTag = "hub_sec_advanced",
                                onClick = { selectedSection = HubSection.ADVANCED }
                            )
                        }
                    }
                }
                when (selectedSection) {
                    HubSection.COMPANY_INFO -> CompanyInfoSection(defaultConfig, viewModel) { selectedSection = null }
                    HubSection.SHIFTS -> ShiftManagementDialogContent(viewModel.shiftRepository, companyId) { selectedSection = null }
                    HubSection.WORK_RULES -> WorkRuleManagementDialogContent(viewModel.workRuleRepository, companyId) { selectedSection = null }
                    HubSection.OVERTIME_RULES -> OvertimeRuleManagementDialogContent(viewModel.overtimeRuleRepository, companyId) { selectedSection = null }
                    HubSection.HOLIDAYS -> HolidaysSection(companyId) { selectedSection = null }
                    HubSection.ALLOWANCES -> AllowancesSection(defaultConfig, viewModel) { selectedSection = null }
                    HubSection.ADVANCED -> AdvancedSection(defaultConfig, viewModel) { selectedSection = null }
                    null -> {}
                }
            }
        }
    }
}"""

open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(re.sub(pattern, replacement, content))
