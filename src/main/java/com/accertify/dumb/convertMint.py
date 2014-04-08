#!/usr/bin/python
import csv
import sys

reader = csv.reader(sys.stdin, delimiter=',', quotechar='"')
writer = csv.writer(sys.stdout,delimiter='|', lineterminator='\n')

for row in reader:
  if float(row[4]) < 0:
    writer.writerow([row[0], row[2], row[3], -1*float(row[4]), 'debit', 'NONE', 'BOFA', '', ''])

mrose@zeta:~/Documents/finance$ cat convertMint.py
#!/usr/bin/python
import csv
import sys

reader = csv.reader(sys.stdin, delimiter=',', quotechar='"')
writer = csv.writer(sys.stdout,delimiter='|', lineterminator='\n')

for row in reader:
  writer.writerow(row)

