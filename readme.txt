Kyle Geib
CSS 434 Parallel and Distributed Computing, Dr. Munehiro Fukuda
Program 4: Distributed File System
December 2012

If you're reading this and looking to cheat off my code... Remember...
    
    "You're only hurting yourself."

That, and you'll not be able to put this DFS file server project on your
resume, robbing you of a great talking point with a potential recruiter.

    "Winners don't do drugs... Unless it's steroids."


How to run!

1) SSH
If doing this on a Linux lab machine, you can use the following commands for
full emacs functionality.
For each terminal:
    $ export DISPLAY=:0
    $ ssh -X -l <uwID> uw1-320-xx
    
Otherwise, you should dedicate a terminal (if through PuTTY) to opening your
text files in pico/nano/emacs.

2) Navigate to the folder that holds all of the .java files.

3) Compile
[Any terminal]
$ javac -d . *.java
$ rmic main.DFSServer main.DFSClient

4) Start RMI Registry
[Client Terminal AND Server Terminal]
$ rmiregistry <port>&

5) Run Server program
[Server Terminal]
java main.DFSServer <port>

6) Run client program
[Client Terminal 1]
java main.DFSClient <accountName> <serverIP> <port>

** IPs are formatted like "uw1-320-09"
** Port must match between all machines and all commands.

7) Kill rmi runtime thing
$ fg
<hit ctrl+c>

NOTES:

There is no client quitting functionality. Exiting a client within a single
run of these programs will likely lead to errors or crashes with the server.