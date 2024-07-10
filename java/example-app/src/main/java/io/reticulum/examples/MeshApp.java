package io.reticulum.examples;

import io.reticulum.Reticulum;
import io.reticulum.Transport;
import io.reticulum.destination.Destination;
import io.reticulum.destination.DestinationType;
import io.reticulum.destination.Direction;
import io.reticulum.destination.ProofStrategy;
//import io.reticulum.destination.ProofStrategy;
import io.reticulum.identity.Identity;
import io.reticulum.link.Link;
//import io.reticulum.constant.LinkConstant;
import io.reticulum.packet.Packet;
import io.reticulum.packet.PacketReceipt;
import io.reticulum.packet.PacketReceiptStatus;
import io.reticulum.transport.AnnounceHandler;
import static io.reticulum.link.TeardownSession.DESTINATION_CLOSED;
import static io.reticulum.link.TeardownSession.INITIATOR_CLOSED;
import static io.reticulum.link.TeardownSession.TIMEOUT;
import static io.reticulum.link.LinkStatus.ACTIVE;
//import static io.reticulum.packet.PacketContextType.LINKCLOSE;
import static io.reticulum.identity.IdentityKnownDestination.recall;
import static io.reticulum.utils.IdentityUtils.concatArrays;
//import static io.reticulum.constant.ReticulumConstant.TRUNCATED_HASHLENGTH;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
//import lombok.Setter;
//import lombok.Getter;
import lombok.Synchronized;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
//import static java.util.Objects.isNull;
//import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
//import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.ArrayUtils.subarray;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;

import org.apache.commons.cli.*;

@Data
@Slf4j
public class MeshApp {
    private static final String APP_NAME = "example_utilities";
    Reticulum reticulum;
    //static final String defaultConfigPath = new String(".reticulum");
    Identity meshIdentity;
    Transport transport;
    public Destination baseDestination;
    private volatile boolean isShuttingDown = false;
    private final List<RNSPeer> linkedPeers = Collections.synchronizedList(new ArrayList<>());
    private final List<Link> incomingLinks = Collections.synchronizedList(new ArrayList<>());
    //public Link latestClientLink;
    //public Link meshLink;
    //public Link clientLink;
    
    /************/
    /** Mesh   **/
    /************/
    private void meshSetup(String configPath) {

        try {
            reticulum = new Reticulum(configPath);
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }

        // create identity either from file or new (creating new keys)
        var meshIdentityPath = reticulum.getStoragePath().resolve("identities/"+APP_NAME);
        if (Files.isReadable(meshIdentityPath)) {
            meshIdentity = Identity.fromFile(meshIdentityPath);
            log.info("mesh identity loaded from file {}", meshIdentityPath.toString());
        } else {
            meshIdentity = new Identity();
            log.info("new mesh identity created dynamically.");
        }

        // We create a destination that clients can connect to. We
        // want clients to create links to this destination, so we
        // need to create a "single" destination type.
        baseDestination = new Destination(
            meshIdentity,
            Direction.IN,
            DestinationType.SINGLE,
            APP_NAME,
            "meshexample"
        );
        //log.info("destination1 hash: "+destination1.getHexHash());
   
        baseDestination.setProofStrategy(ProofStrategy.PROVE_ALL);
        baseDestination.setAcceptLinkRequests(true);

        // We configure a function that will get called every time
        // a new client creates a link to this destination.
        baseDestination.setLinkEstablishedCallback(this::clientConnected);
        
        Transport.getInstance().registerAnnounceHandler(new MeshAnnounceHandler());
        log.info("announceHandlers: {}", Transport.getInstance().getAnnounceHandlers());

        meshLoop(baseDestination);
    }

    public void meshLoop(Destination destination) {
        var inData = new String();
        log.info("***> mesh instance * {} * running, waiting for a connection", destination.getHexHash());

        log.info("Hit enter to manually send an announce (Ctrl-C or 'quit' to quit, 'help' or '?' for keyword usage)");

        Scanner scan = new Scanner( System.in );
        System.out.print("> ");
        while (true) {
            try {
                inData = scan.nextLine();
                if (inData.equalsIgnoreCase("quit")) {
                    shutdown();
                    scan.close();
                    break;
                } else if (inData.isEmpty()) {
                    destination.announce("mesh-node".getBytes());
                    log.info("Sent announce from {} ({})", destination.getHexHash(), destination.getName());
                    System.out.println("> ");
                } else if (inData.equalsIgnoreCase("help") || inData.equals("?")) {
                    log.info("**********************************  keywords  **********************************");
                    log.info("=> press Enter    to do a Reticulum announce (and initiate peers to connect)");
                    log.info("=> enter 'probe' to ping peers to probe remote availability (closes peerLink on timeout)");
                    log.info("=> enter 'status' to see status of peer links");
                    log.info("=> enter some text (other than keywords) to send message to peers");
                    log.info("=> enter 'close'/'open' to teardown/re-open existing peer links");
                    log.info("=> enter 'clean' to shutdown and remove all non-ACTIVE local peer objects");
                    log.info("=> enter 'quit' to exit");
                    log.info("=> enter '?' or 'help' to see this message");
                    log.info("********************************************************************************");
                } else {
                    //var rand = new Random();
                    //var randomPeer = linkedPeers.get(rand.nextInt(linkedPeers.size()));
                    if (linkedPeers.isEmpty()) {
                        log.info("no local peer objects (yet). We'll create on for every announce we receive.");
                    } else {
                        for (RNSPeer p: linkedPeers) {
                            var rpl = p.getPeerLink();
                            if (inData.equalsIgnoreCase("probe")) {
                                p.pingRemote();
                            } else if (inData.equalsIgnoreCase("close")) {
                                //sendCloseToRemote(rpl); // note: this has no effect
                                rpl.teardown();
                                log.info("peerLink: {} - status: {}", rpl, rpl.getStatus());
                            } else if (inData.equalsIgnoreCase("open")) {
                                p.getOrInitPeerLink();
                                log.info("peerLink: {} - status: {}", p.getPeerLink(), p.getPeerLink().getStatus());
                            } else if (inData.equalsIgnoreCase("clean") && (p.getPeerLink().getStatus() != ACTIVE)) {
                                //p.shutdown();
                                linkedPeers.remove(p);
                            } else if (inData.equalsIgnoreCase("status")) {
                                log.info("peer destinationHash: {}, peerLink: {} <=> status: {}",
                                    Hex.encodeHexString(p.getDestinationHash()),
                                    p.getPeerLink(), p.getPeerLink().getStatus());
                                    continue;
                            } else {
                                if (rpl.getStatus() == ACTIVE) {
                                var data = inData.getBytes(UTF_8);
                                log.info("sending text \"{}\" to peer: {}", inData, Hex.encodeHexString(p.getDestinationHash()));
                                var testPacket = new Packet(rpl, data);
                                testPacket.send();
                                } else {
                                    log.info("can't send data to link with status: {}", rpl.getStatus());
                                }
                            }
                        }
                        if (inData.equalsIgnoreCase("status")) {
                            log.info("we have {} non-initiator links, list: {}", incomingLinks.size(), incomingLinks);
                            //for (Link l: incomingLinks) {
                            //    log.info("incoming link {}, destination: {}", l, Hex.encodeHexString(l.getDestination().getHash()));
                            //}
                        }
                    }
                }
            } catch (Exception e) {
                scan.close();
            }
        }
        System.exit(0);
    }

    public void shutdown() {
        isShuttingDown = true;
        log.info("shutting down Reticulum");
        for (RNSPeer p: linkedPeers) {
            log.info("shutting down peer: {}", p);
            p.shutdown();
            try {
                TimeUnit.SECONDS.sleep(1); // allow for peers to disconnect gracefully
            } catch (InterruptedException e) {
                log.error("exception: {}", e);
            }
        }
        // gracefully close links of peers that point to us
        for (Link l: incomingLinks) {
            sendCloseToRemote(l);
        }
        // Note: we still need to get the packet timeout callback to work...
        reticulum.exitHandler();
    }

    public void sendCloseToRemote(Link link) {
        var data = concatArrays("close::".getBytes(UTF_8),link.getDestination().getHash());
        Packet closePacket = new Packet(link, data);
        var packetReceipt = closePacket.send();
        packetReceipt.setTimeout(3L);
        packetReceipt.setDeliveryCallback(this::closePacketDelivered);
        packetReceipt.setTimeoutCallback(this::packetTimedOut);
    }

    public void closePacketDelivered(PacketReceipt receipt) {
        var rttString = new String("");
        if (receipt.getStatus() == PacketReceiptStatus.DELIVERED) {
            var rtt = receipt.getRtt();    // rtt (Java) is in miliseconds
            //log.info("qqp - packetDelivered - rtt: {}", rtt);
            if (rtt >= 1000) {
                rtt = Math.round(rtt / 1000);
                rttString = String.format("%d seconds", rtt);
            } else {
                rttString = String.format("%d miliseconds", rtt);
            }
            log.info("Shutdown packet confirmation received from {}, round-trip time is {}",
                    Hex.encodeHexString(receipt.getDestination().getHash()), rttString);
        }
    }

    public void packetTimedOut(PacketReceipt receipt) {
        log.info("packet timed out");
        if (receipt.getStatus() == PacketReceiptStatus.FAILED) {
            log.info("packet timed out, receipt status: {}", PacketReceiptStatus.FAILED);
        }
    }

    public void clientConnected(Link link) {
        link.setLinkClosedCallback(this::clientDisconnected);
        link.setPacketCallback(this::serverPacketReceived);
        var peer = findPeerByLink(link);
        if (nonNull(peer)) {
            log.info("initiator peer {} opened link (link lookup: {}), link destination hash: {}",
                Hex.encodeHexString(peer.getDestinationHash()), link, Hex.encodeHexString(link.getDestination().getHash()));
            // make sure the peerLink is active.
            peer.getOrInitPeerLink();
        } else {
            log.info("non-initiator opened link (link lookup: {}), link destination hash (initiator): {}",
                peer, link, Hex.encodeHexString(link.getDestination().getHash()));
        }
        incomingLinks.add(link);
        log.info("***> Client connected, link: {}", link);
    }

    public void clientDisconnected(Link link) {
        var peer = findPeerByLink(link);
        if (nonNull(peer)) {
            log.info("initiator peer closed link (link lookup: {}), link destination hash: {}",
                Hex.encodeHexString(peer.getDestinationHash()), link, Hex.encodeHexString(link.getDestination().getHash()));
        } else {
            log.info("non-initiator closed link (link lookup: {}), link destination hash (initiator): {}",
                peer, link, Hex.encodeHexString(link.getDestination().getHash()));
        }
        // if we have a peer pointing to that destination, we can close and remove it
        peer = findPeerByDestinationHash(link.getDestination().getHash());
        if (nonNull(peer)) {
            // Note: no shutdown as the remobe peer could be only rebooting.
            //       keep it to reopen link later if possible.
            peer.getPeerLink().teardown();
        }
        incomingLinks.remove(link);
        log.info("***> Client disconnected");
    }

    public void serverPacketReceived(byte[] message, Packet packet) {
        String text = new String(message, StandardCharsets.UTF_8);
        log.info("Received data on the link: \"{}\"", text);
        //if (text.startsWith("close::")) {
        //    var targetPeerHash = subarray(message, 6, message.length);
        //    var peer = findPeerByDestinationHash(targetPeerHash);
        //    if (nonNull(peer)) {
        //        log.info("found peer matching close packet - closing link for: {}", targetPeerHash);
        //        peer.getPeerLink().teardown();
        //    }
        //}
        //var peer = findPeerByDestinationHash(packet.getDestinationHash());
        //// send reply
        //if (nonNull(peer)) {
        //  String replyText = "pong";
        //  byte[] replyData = replyText.getBytes(StandardCharsets.UTF_8);
        //  Packet reply = new Packet(peer.getPeerLink(), replyData);
        //}
    }

    @Synchronized
    public void prunePeers(Link link) {
        List<RNSPeer> lps =  getLinkedPeers();
        log.info("number of peers before pruning: {}", lps.size());
        for (RNSPeer p: lps) {
            log.info("peerLink: {}", p.getPeerLink());
            if (p.getPeerLink() == null) {
                log.info("link is null, removing peer");
                lps.remove(p);
                continue;
            }
        }
        log.info("number of peers after pruning: {}, {}", lps.size(), getLinkedPeers().size());
    }

    public RNSPeer findPeerByLink(Link link) {
        // find peer given a link. At the same time remove peers with no peerLink.
        List<RNSPeer> lps =  getLinkedPeers();
        RNSPeer peer = null;
        for (RNSPeer p : lps) {
            var pLink = p.getPeerLink();
            if (nonNull(pLink)) {
                log.info("* findPeerByLink - peerLink hash: {}, link destination hash: {}",
                        Hex.encodeHexString(pLink.getDestination().getHash()),
                        Hex.encodeHexString(link.getDestination().getHash()));
                if (Arrays.equals(pLink.getDestination().getHash(),link.getDestination().getHash())) {
                    log.info("  findPeerByLink - found peer matching destinationHash");
                    peer = p;
                    break;
                }
            }
        }
        return peer;
    }

    public RNSPeer findPeerByDestinationHash(byte[] dhash) {
        List<RNSPeer> lps = getLinkedPeers();
        RNSPeer peer = null;
        for (RNSPeer p : lps) {
            var pLink = p.getPeerLink();
            log.info("* findPeerByDestinationHash - peerLink destination hash: {}, search hash: {}",
                    Hex.encodeHexString(pLink.getDestination().getHash()),
                    Hex.encodeHexString(dhash));
            if (Arrays.equals(p.getDestinationHash(), dhash)) {
                log.info("  findPeerByDestinationHash - found peer matching destinationHash");
                peer = p;
                break;
            }
        }
        return peer;
    }

    /***********************/
    /** AnnounceHandler   **/
    /***********************/

    private class MeshAnnounceHandler implements AnnounceHandler {
        @Override
        public String getAspectFilter() {
            return null;
        }

        @Override
        @Synchronized
        public void receivedAnnounce(byte[] destinationHash, Identity announcedIdentity, byte[] appData) {
            var peerExists = false;

            log.info("Received an announce from {}", Hex.encodeHexString(destinationHash));

            if (nonNull(appData)) {
                log.debug("The announce contained the following app data: {}", new String(appData, UTF_8));
            }

            List<RNSPeer> lps =  getLinkedPeers();
            for (RNSPeer p : lps) {
                //if (isNull(p.getPeerLink())) {
                //    log.info("peer link no longer availabe, removing peer") {
                //        linkedPeers.remove(p);
                //        continue;
                //    }
                //}
                if (Arrays.equals(p.getDestinationHash(), destinationHash)) {
                    log.info("MeshAnnounceHandler - peer exists - found peer matching destinationHash");
                    if (nonNull(p.getPeerLink())) {
                        log.info("peer link: {}, status: {}", p.getPeerLink(), p.getPeerLink().getStatus());
                    }
                    peerExists = true;
                    if (p.getPeerLink().getStatus() != ACTIVE) {
                        p.getOrInitPeerLink();
                    }
                    break;
                } else {
                    log.info("MeshAnnounceHandler - no matching peer,  peerLink hash: {}, link destination hash: {}",
                            Hex.encodeHexString(p.getDestinationHash()),
                            Hex.encodeHexString(destinationHash));
                    if (nonNull(p.getPeerLink())) {
                        log.info("peer link: {}, status: {}", p.getPeerLink(), p.getPeerLink().getStatus());
                    }
                }
            }
            //if (peer == null) {
            if (!peerExists) {
                RNSPeer newPeer = new RNSPeer(destinationHash);
                newPeer.setServerIdentity(announcedIdentity);
                newPeer.setIsInitiator(true);
                lps.add(newPeer);
                log.info("added new RNSPeer, destinationHash: {}", Hex.encodeHexString(destinationHash));
            }
        }
    }

    /************/
    /** Peer   **/
    /************/
    @Data
    private class RNSPeer {
        byte[] destinationHash;
        Destination peerDestination;
        Identity serverIdentity;
        Long creationTimestamp;
        Long lastAccessTimestamp;
        Boolean isInitiator;
        Link peerLink;
        Reticulum rns = reticulum;

        public RNSPeer(byte[] dhash) {
            this.destinationHash = dhash;
            this.serverIdentity = recall(dhash);
            initPeerLink();
        }

        public void initPeerLink() {
            peerDestination = new Destination(
                serverIdentity,
                Direction.OUT, 
                DestinationType.SINGLE,
                APP_NAME,
                "meshexample"
            );
            peerDestination.setProofStrategy(ProofStrategy.PROVE_ALL);

            setCreationTimestamp(System.currentTimeMillis());
            lastAccessTimestamp = null;
            isInitiator = true;

            peerLink = new Link(peerDestination);

            peerLink.setLinkEstablishedCallback(this::linkEstablished);
            peerLink.setLinkClosedCallback(this::linkClosed);
            peerLink.setPacketCallback(this::linkPacketReceived);
        }

        public Link getOrInitPeerLink() {
            if (this.peerLink.getStatus() == ACTIVE) {
                return this.peerLink;
            } else {
                initPeerLink();
            }
            return this.peerLink;
        }

        public void shutdown() {
            if (nonNull(peerLink)) {
                log.info("shutdown - peerLink: {}, status: {}", peerLink, peerLink.getStatus());
                if (peerLink.getStatus() == ACTIVE) {
                    peerLink.teardown();
                }
                // else {
                //    peerLink = null;
                //}
                this.peerLink = null;
            }
        }

        public void linkEstablished(Link link) {
            link.setLinkClosedCallback(this::linkClosed);
            log.info("peerLink {} established (link: {}) with peer: hash - {}, link destination hash: {}", 
                peerLink, link, Hex.encodeHexString(destinationHash),
                Hex.encodeHexString(link.getDestination().getHash()));
        }

        public void linkClosed(Link link) {
            if (link.getTeardownReason() == TIMEOUT) {
                log.info("The link timed out");
            } else if (link.getTeardownReason() == INITIATOR_CLOSED) {
                log.info("Link closed callback: The initiator closed the link");
                log.info("peerLink {} closed (link: {}), link destination hash: {}",
                    peerLink, link, Hex.encodeHexString(link.getDestination().getHash()));
            } else if (link.getTeardownReason() == DESTINATION_CLOSED) {
                log.info("Link closed callback: The link was closed by the peer, removing peer");
                log.info("peerLink {} closed (link: {}), link destination hash: {}",
                    peerLink, link, Hex.encodeHexString(link.getDestination().getHash()));
            } else {
                log.info("Link closed callback");
            }
        }

        public void linkPacketReceived(byte[] message, Packet packet) {
            var msgText = new String(message, StandardCharsets.UTF_8);
            if (msgText.equals("ping")) {
                log.info("received ping on link");
            } else if (msgText.startsWith("close::")) {
                var targetPeerHash = subarray(message, 7, message.length);
                log.info("peer dest hash: {}, target hash: {}",
                    Hex.encodeHexString(destinationHash),
                    Hex.encodeHexString(targetPeerHash));
                if (Arrays.equals(destinationHash, targetPeerHash)) {
                    log.info("closing link: {}", peerLink.getDestination().getHexHash());
                    peerLink.teardown();
                }
            } else if (msgText.startsWith("open::")) {
                var targetPeerHash = subarray(message, 7, message.length);
                log.info("peer dest hash: {}, target hash: {}",
                    Hex.encodeHexString(destinationHash),
                    Hex.encodeHexString(targetPeerHash));
                if (Arrays.equals(destinationHash, targetPeerHash)) {
                    log.info("closing link: {}", peerLink.getDestination().getHexHash());
                    getOrInitPeerLink();
                }
            }
        }

        // PacketReceipt callbacks
        public void packetTimedOut(PacketReceipt receipt) {
            log.info("packet timed out");
            if (receipt.getStatus() == PacketReceiptStatus.FAILED) {
                log.info("packet timed out, receipt status: {}", PacketReceiptStatus.FAILED);
                peerLink.teardown();
            }
        }

        public void packetDelivered(PacketReceipt receipt) {
            var rttString = new String("");
            //log.info("packet delivered callback, receipt: {}", receipt);
            if (receipt.getStatus() == PacketReceiptStatus.DELIVERED) {
                var rtt = receipt.getRtt();    // rtt (Java) is in miliseconds
                if (rtt >= 1000) {
                    rtt = Math.round(rtt / 1000);
                    rttString = String.format("%d seconds", rtt);
                } else {
                    rttString = String.format("%d miliseconds", rtt);
                }
                log.info("Valid reply received from {}, round-trip time is {}",
                        Hex.encodeHexString(receipt.getDestination().getHash()), rttString);
            }
        }

        // utility methods
        public void pingRemote() {
            var link = this.peerLink;
            if (nonNull(UTF_8)) {
                if (peerLink.getStatus() == ACTIVE) {
                    log.info("pinging remote: {}", link);
                    var data = "ping".getBytes(UTF_8);
                    link.setPacketCallback(this::linkPacketReceived);
                    Packet pingPacket = new Packet(link, data);
                    PacketReceipt packetReceipt = pingPacket.send();
                    packetReceipt.setTimeout(3L);
                    packetReceipt.setTimeoutCallback(this::packetTimedOut);
                    packetReceipt.setDeliveryCallback(this::packetDelivered);
                } else {
                    log.info("can't send ping to a peer {} with (link) status: {}",
                        Hex.encodeHexString(peerLink.getDestination().getHash()), peerLink.getStatus());
                }
            }
        }
    }


    /*********/
    /** Main */
    /*********/
    public static void main(String[] args) throws IOException {
        // main to run application directly

        String cmdUsage = new String("run_mesh.sh [-h|-config PATH]");

         String defaultConfigPath = new String(".reticulum");
        
        // define options
        Options options = new Options();
        Option o_help = Option.builder("h").longOpt("help")
                            .argName("usage")
                            .hasArg(false)
                            .required(false)
                            .desc("program usage")
                            .build();
        options.addOption(o_help);
        Option o_config = Option.builder("c").longOpt("config")
                            .argName("dir")
                            .hasArg(true)
                            .required(false)
                            .desc("(optional) path to alternative Reticulum config directory "
                                  + "(default: .reticulum)").build();
        options.addOption(o_config);
        // define parser
        CommandLine cLine;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        
        try {
            cLine = parser.parse(options, args);
        
            if (cLine.hasOption("h")) {
                formatter.printHelp(cmdUsage, options);
                System.exit(0);
            }

            var instance = new MeshApp();

            if (cLine.hasOption("config")) {
                try {
                    instance.meshSetup(cLine.getOptionValue("c"));
                } catch (IllegalArgumentException e) {
                    log.error("Invalid config path. Check your input!");
                    System.exit(0);
                }
            } else {
                instance.meshSetup(defaultConfigPath);
            }

        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            formatter.printHelp(cmdUsage, options);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
