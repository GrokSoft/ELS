
# ELS: Testing Notes

These are rough note.

## Command lines
General parameters:
 * Start from the "mock" directory
 * Main class: com.groksoft.els.Main
 * VM options: -Dlog4j.configurationFile=file:../lib/log4j2.xml

1. Bad arguments test<br/>
   Simple error handling test.<br/>
   -p

2. Full munge dry run<br/>
   Dry run munge with scans of publisher and subscriber<br/>
   -c off -D -d info -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -m TestRun/mismatches.txt -w TestRun/whatsnew.txt

3. Full munge<br/> 
   Full munge with scans of publisher and subscriber<br/>
   -c off -d debug -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -m TestRun/mismatches.txt -w TestRun/whatsnew.txt

4. Publisher export collection<br/>
   Export publisher collection for next test<br/>
   -p TestRun/publisher/publisher-libraries.json -i TestRun/publisher-export.json

5. Munge dry run -P import publisher<br/>
   Dry run munger importing publisher collection file, scanning subscriber<br/> 
   -D -P TestRun/publisher-export.json -s TestRun/subscriber-1/subscriber-1-libraries.json





7. Remote subscriber -r S listener<br/>
   Subscriber listening, will scan when requested by publisher (next)<br/> 
   -a 1234 -d debug -r S -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -f TestRun/els-subscriber.log

8. Remote publisher -r P request collection and targets<br/>
   Publisher munge, local scan, requesting subscriber collection and targets<br/>
   -d debug -r P -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -t TestRun/targets-1.json -m TestRun/mismatches.txt -w TestRun/whatsnew.txt -f TestRun/els-publisher.log

9. Subscriber export collection<br/> 
   Export subscriber collection for next test<br/>
   -p TestRun/subscriber-1/subscriber-1-libraries.json -i TestRun/subscriber-export.json

10. Remote subscriber -r S force collection and targets<br/>
    Subscriber listening, forcing loaded collection from previous export and targets<br/>
   -a 1234 -d debug -r S -p TestRun/publisher/publisher-libraries.json -S TestRun/subscriber-export.json -T TestRun/targets-1.json -f TestRun/els-subscriber.log

11. Remote publisher -r P publish<br/>
    Publisher munge, local subscriber libraries overridden by forced subscriber collection and forced targets<br/>
   -d debug -r P -p TestRun/publisher/publisher-libraries.json -S TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -m TestRun/mismatches.txt -w TestRun/whatsnew.txt -f TestRun/els-publisher.log

12. Remote publisher manually -r M<br/>
    Interactive (manual) terminal to a remote subscriber<br/>
   -d debug -r M -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -f TestRun/els-publisher-manual.log
