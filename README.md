SchoolManager v1.33 - Monthly Selection Divisor Fix

Changes:
- Month selection now allows any number of selected months from 1 to 12 (current calendar year only).
- The monthly average divisor is the number of months selected by the teacher.
- Missing monthly records count as 0 for selected months, so the divisor remains the selected-month count.
- Example: one selected month total 89 => 89 / 1 = 89. Two selected months with totals 115 and 120 => 235 / 2 = 117.50.

Build with the project's usual GitHub Actions workflow. A local Android Gradle build was not run in this environment.
