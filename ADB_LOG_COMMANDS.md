# Callify — ADB Call Log Command Reference

## Requirements
- Debug build installed (`com.callify.debug`)
- USB debugging enabled on device
- ADB installed on host machine

---

## Connect to the Database

```bash
adb shell run-as com.callify.debug sqlite3 \
  /data/data/com.callify.debug/databases/callify_contacts.db
```

Or as a one-liner prefix for all commands below:
```bash
DB="run-as com.callify.debug sqlite3 \
  /data/data/com.callify.debug/databases/callify_contacts.db"
```

---

## View All Logs

```sql
SELECT * FROM call_log ORDER BY date DESC, time DESC;
```

Pretty-printed with headers:
```bash
adb shell run-as com.callify.debug sqlite3 \
  -header -column \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "SELECT * FROM call_log ORDER BY date DESC, time DESC;"
```

---

## Filter by Date

Single day:
```sql
SELECT * FROM call_log WHERE date = '2025-06-01';
```

Date range:
```sql
SELECT * FROM call_log
WHERE date BETWEEN '2025-06-01' AND '2025-06-30'
ORDER BY date DESC, time DESC;
```

Today only:
```sql
SELECT * FROM call_log
WHERE date = DATE('now')
ORDER BY time DESC;
```

---

## Filter by Caller Type

Identified callers only (names contain a space, not a raw number):
```sql
SELECT * FROM call_log
WHERE caller NOT GLOB '[0-9]*'
ORDER BY date DESC, time DESC;
```

Unknown / unidentified numbers only:
```sql
SELECT * FROM call_log
WHERE caller GLOB '[0-9]*'
ORDER BY date DESC, time DESC;
```

Specific caller by name:
```sql
SELECT * FROM call_log
WHERE caller LIKE '%Alaye%'
ORDER BY date DESC, time DESC;
```

Specific number:
```sql
SELECT * FROM call_log
WHERE caller = '9074086115';
```

---

## Filter by Receiver Number

```sql
SELECT * FROM call_log
WHERE receiver_number = '8033006821';
```

---

## Count Queries

Total calls logged:
```sql
SELECT COUNT(*) AS total_calls FROM call_log;
```

Calls per day:
```sql
SELECT date, COUNT(*) AS calls
FROM call_log
GROUP BY date
ORDER BY date DESC;
```

Identified vs unknown breakdown:
```sql
SELECT
  SUM(CASE WHEN caller NOT GLOB '[0-9]*' THEN 1 ELSE 0 END) AS identified,
  SUM(CASE WHEN caller GLOB '[0-9]*' THEN 1 ELSE 0 END)     AS unknown
FROM call_log;
```

---

## Export to CSV

All records:
```bash
adb shell run-as com.callify.debug sqlite3 \
  -header -csv \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "SELECT * FROM call_log ORDER BY date DESC, time DESC;" \
  > callify_call_log.csv
```

Filtered by date range:
```bash
adb shell run-as com.callify.debug sqlite3 \
  -header -csv \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "SELECT * FROM call_log WHERE date BETWEEN '2025-06-01' AND '2025-06-30';" \
  > callify_june_2025.csv
```

Identified callers only:
```bash
adb shell run-as com.callify.debug sqlite3 \
  -header -csv \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "SELECT * FROM call_log WHERE caller NOT GLOB '[0-9]*';" \
  > callify_identified.csv
```

Unknown numbers only:
```bash
adb shell run-as com.callify.debug sqlite3 \
  -header -csv \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "SELECT * FROM call_log WHERE caller GLOB '[0-9]*';" \
  > callify_unknown.csv
```

---

## Export to PDF (via host machine)

After exporting CSV, convert on your machine:

**macOS / Linux:**
```bash
# Install csvkit if needed: pip install csvkit
csvlook callify_call_log.csv | enscript -p callify_call_log.ps
ps2pdf callify_call_log.ps callify_call_log.pdf
```

**Or using Python (cross-platform):**
```python
import pandas as pd
from fpdf import FPDF

df = pd.read_csv("callify_call_log.csv")

pdf = FPDF()
pdf.add_page()
pdf.set_font("Arial", size=10)
pdf.set_fill_color(30, 30, 30)

col_widths = [25, 22, 65, 45]
headers = ["Date", "Time", "Caller", "Receiver"]

# Header row
pdf.set_font("Arial", "B", 10)
for i, h in enumerate(headers):
    pdf.cell(col_widths[i], 8, h, border=1)
pdf.ln()

# Data rows
pdf.set_font("Arial", size=9)
for _, row in df.iterrows():
    pdf.cell(col_widths[0], 7, str(row["date"]),            border=1)
    pdf.cell(col_widths[1], 7, str(row["time"]),            border=1)
    pdf.cell(col_widths[2], 7, str(row["caller"])[:30],     border=1)
    pdf.cell(col_widths[3], 7, str(row["receiver_number"]), border=1)
    pdf.ln()

pdf.output("callify_call_log.pdf")
print("Exported: callify_call_log.pdf")
```

Install dependencies: `pip install fpdf2 pandas`

---

## Pull the Raw Database File to Host

If you prefer to query locally with DB Browser for SQLite
or any other desktop SQLite tool:

```bash
adb shell run-as com.callify.debug cat \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  > callify_local_copy.db
```

Open `callify_local_copy.db` in DB Browser for SQLite —
full GUI filtering, sorting, and export available.

---

## Clear the Log (destructive)

```bash
adb shell run-as com.callify.debug sqlite3 \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "DELETE FROM call_log;"
```

Reset auto-increment counter too:
```bash
adb shell run-as com.callify.debug sqlite3 \
  /data/data/com.callify.debug/databases/callify_contacts.db \
  "DELETE FROM call_log; DELETE FROM sqlite_sequence WHERE name='call_log';"
```

---

## Notes

- All timestamps are device local time (not UTC)
- Caller field: identified = "Firstname Lastname",
  unknown = normalised 10-digit number
- Receiver field: "unknown_receiver" means carrier did not
  provision the SIM number or READ_PHONE_NUMBERS was not granted
- Log persists across app restarts and updates (debug build)
- Log is wiped on app uninstall
