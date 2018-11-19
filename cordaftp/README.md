<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Corda FTP

Based on the Corda template (so don't be surprised if there are still some template artifacts hidden).

## Background

This is a very basic demonstration of using Corda to transfer files from one node to another. It is best run via
IntelliJ. There are two parts to the demonstration; the Corda Nodes and the client application that scans directories
and sends the files to the other nodes. In this demonstration, we will be using a pre-configured set of nodes and
directories but once you have it running, feel free to expand this and let us know how you get on.

## Instructions

1. Clone the repo from https://github.com/corda/cordaftp
2. Open this directory as a project in IntelliJ
3. Start the "Run Corda FTP Nodes" run configuration in the dropdown
4. Wait until the nodes have started up (i.e. there is no more activity in the output window)
5. Start the "SenderKt" program
6. Using the command shell or GUI explorer, create a directory under `build` called `send1`
7. Create a file that ends in a .txt suffix with some data
8. Watch the output of either of the running sessions (either the client log or the node logs) and wait ...
9. The file will disappear from that directory / node and appear in another directory being transferred via the Corda 
   network that you are running. The destination directory will appear under build/DATETIMEOFBUILD/CorpA/incoming_1


It's a very simple example of a CorDapp using attachments but do give us feedback on how you get on and feel free to clone and PR any modifications you make.

### Notes

When you are sending a file, the receiving process is unintelligent and will just overwrite any file on the destination of that same name.

There is a .json configuration file that is read in by the application. This is split into two major parts "txMap" and "rxMap". The important thing to note is that the only thing that the configurations need to match on is the "reference", and that this matches up the sender with the receiver. In the example, CorpB is sending files to CorpA under two configurations; the only significant difference is that files sent under "CorpA1" reference are deleted from CorpB's filesystem once they have been successfully received by CorpA. The example configuration demonstrates how to include multiple source directories and match patterns in one configuration file (hence the two directories).

There is an arbitrary file limit of 5MB (for now).



