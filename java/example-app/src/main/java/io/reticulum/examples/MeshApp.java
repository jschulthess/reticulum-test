package io.reticulum.examples;

import io.reticulum.Reticulum;
import io.reticulum.Transport;
import io.reticulum.interfaces.ConnectionInterface;
import io.reticulum.destination.Destination;
import io.reticulum.destination.DestinationType;
import io.reticulum.destination.Direction;
import io.reticulum.destination.ProofStrategy;
//import io.reticulum.destination.ProofStrategy;
import io.reticulum.identity.Identity;
import io.reticulum.link.Link;
import io.reticulum.constant.LinkConstant;
import io.reticulum.buffer.Buffer;
import io.reticulum.buffer.BufferedRWPair;
import io.reticulum.channel.Channel;
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
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
//import static java.util.Objects.isNull;
//import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
//import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

import java.util.Base64;
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
    //private static Boolean isTest = true;
    //private static final String APP_NAME = isTest ? "example_utilities_test": "example_utilities";
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
    Boolean useBuffer = false;
    
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
            // save it back to file by default for next start (possibly add setting to override)
            try {
                var identitiesPath = reticulum.getStoragePath().resolve("identities");
                if (Files.notExists(identitiesPath)) {
                    Files.createDirectories(identitiesPath);
                }
                Files.write(meshIdentityPath, meshIdentity.getPrivateKey(), CREATE, WRITE);
                log.info("serverIdentity written back to file");
            } catch (IOException e) {
                log.error("Error while saving serverIdentity to {}", meshIdentityPath, e);
            }
        }

        // show the ifac_size of the configured interfaces (debug code)
        for (ConnectionInterface i: Transport.getInstance().getInterfaces() ) {
            log.info("interface {}, length: {}", i.getInterfaceName(), i.getIfacSize());
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
                                if (useBuffer) {
                                    p.getPeerBuffer().close();
                                    log.info("buffer: {}", p.getPeerBuffer());
                                }
                            } else if (inData.equalsIgnoreCase("open")) {
                                p.getOrInitPeerLink();
                                log.info("peerLink: {} - status: {}", p.getPeerLink(), p.getPeerLink().getStatus());
                                if (useBuffer) {
                                    p.getOrInitPeerBuffer();
                                    log.info("buffer: {}", p.getPeerBuffer());
                                }
                            } else if (inData.equalsIgnoreCase("clean")) {
                                if (p.getPeerLink().getStatus() != ACTIVE) {
                                    //p.shutdown();
                                    linkedPeers.remove(p);
                                }
                            } else if (inData.equalsIgnoreCase("status")) {
                                log.info("peer destinationHash: {}, peerLink: {} <=> status: {}",
                                    Hex.encodeHexString(p.getDestinationHash()),
                                    p.getPeerLink(), p.getPeerLink().getStatus());
                                if (useBuffer) {
                                    log.info("peer buffer: {}", p.getPeerBuffer());
                                }
                                continue;
                            } else {
                                if (rpl.getStatus() == ACTIVE) {
                                    var data = inData.getBytes(UTF_8);
                                    log.info("sending text \"{}\" to peer: {}", inData, Hex.encodeHexString(p.getDestinationHash()));
                                    if (useBuffer) {
                                        //var peerBuffer = p.getOrInitPeerBuffer();
                                        var peerBuffer = p.getPeerBuffer();
                                        peerBuffer.write(data);
                                        peerBuffer.flush();
                                    } else {
                                        if (data.length <= LinkConstant.MDU) {
                                            var testPacket = new Packet(rpl, data);
                                            testPacket.send();
                                        } else {
                                            log.info("Cannot send this packet, the data length of {} bytes exceeds link MDU of {} bytes", data.length, LinkConstant.MDU);
                                        }
                                    }
                                } else {
                                    log.info("can't send data to link with status: {}", rpl.getStatus());
                                }
                            }
                        }
                        //if (inData.equalsIgnoreCase("status")) {
                        //    log.info("we have {} non-initiator links, list: {}", incomingLinks.size(), incomingLinks);
                        //    //for (Link l: incomingLinks) {
                        //    //    log.info("incoming link {}, destination: {}", l, encodeHexString(l.getDestination().getHash()));
                        //    //}
                        //}
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
                // allow for peers to disconnect gracefully
                TimeUnit.SECONDS.sleep(1);
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
        if (nonNull(link)) {
            var data = concatArrays("close::".getBytes(UTF_8),link.getDestination().getHash());
            Packet closePacket = new Packet(link, data);
            var packetReceipt = closePacket.send();
            packetReceipt.setDeliveryCallback(this::closePacketDelivered);
            packetReceipt.setTimeoutCallback(this::packetTimedOut);
        } else {
            log.debug("can't send to null link");
        }
    }

    public void closePacketDelivered(PacketReceipt receipt) {
        var rttString = new String("");
        if (receipt.getStatus() == PacketReceiptStatus.DELIVERED) {
            var rtt = receipt.getRtt();    // rtt (Java) is in miliseconds
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
        log.info("packet timed out, receipt status: {}", receipt.getStatus());
        //if (receipt.getStatus() == PacketReceiptStatus.FAILED) {
        //    log.info("packet timed out, receipt status: {}", PacketReceiptStatus.FAILED);
        //}
    }

    public void clientConnected(Link link) {
        link.setLinkClosedCallback(this::clientDisconnected);
        //if (isFalse(useBuffer)) {
            link.setPacketCallback(this::serverPacketReceived);
        //}
        var peer = findPeerByLink(link);
        if (nonNull(peer)) {
            log.info("initiator peer {} opened link (link lookup: {}), link destination hash: {}",
                Hex.encodeHexString(peer.getDestinationHash()), link, Hex.encodeHexString(link.getDestination().getHash()));
            if (this.useBuffer) {
                log.info("clientConnected -- buffer: {}", peer.getPeerBuffer());
                if (nonNull(peer.getPeerBuffer())) {
                    // close previous buffer
                    peer.getPeerBuffer().close();
                    log.info("clientConnected -- buffer after close old: {}", peer.getPeerBuffer());
                }
                // get new buffer
                peer.getOrInitPeerBuffer();
                log.info("clientConnected -- buffer final: {}", peer.getPeerBuffer());
            }
        }
        else {
            // non-initiator - create peer from link
            List<RNSPeer> lps =  getLinkedPeers();
            RNSPeer newPeer = new RNSPeer(link);
            newPeer.setIsInitiator(false);
            log.info("peer channel status: {}", newPeer.getPeerLink().getStatus());
            // do we need to set sendStreamId/receiveStreamId (?)
            lps.add(newPeer);
            log.info("non-initiator opened link (link lookup: {}), link destination hash (initiator): {}",
                    peer, link, Hex.encodeHexString(link.getDestination().getHash()));
        }
        //else {
        //    log.info("non-initiator opened link (link lookup: {}), link destination hash (initiator): {}",
        //        peer, link, Hex.encodeHexString(link.getDestination().getHash()));
        //}
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
            // Note: no shutdown as the remote peer could be only rebooting.
            //       keep it to reopen link later if possible.
            if (nonNull(peer.getPeerBuffer())) {
                peer.getPeerBuffer().close();
            }
            peer.getPeerLink().teardown();
        }
        incomingLinks.remove(link);
        log.info("***> Client disconnected");
    }

    public void serverPacketReceived(byte[] message, Packet packet) {
        String text = new String(message, StandardCharsets.UTF_8);
        log.info("Received data on the link, message: \"{}\"", text);
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
            //return null;
            return "example_utilities.meshexample";
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
            if (!peerExists) {
                RNSPeer newPeer = new RNSPeer(destinationHash);
                newPeer.setServerIdentity(announcedIdentity);
                newPeer.setIsInitiator(true);
                //log.info("peer link status: {}", newPeer.getPeerLink().getStatus());
                // do we need to set sendStreamId/receiveStreamId (?)
                // we won't init the buffer. We can only do this once the link is established.
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
        BufferedRWPair peerBuffer;
        int receiveStreamId = 0;
        int sendStreamId = 0;
        Reticulum rns = reticulum;

        /**
         * Constructor from Announce Handler (initiator)
         */
        public RNSPeer(byte[] dhash) {
            this.destinationHash = dhash;
            this.serverIdentity = recall(dhash);
            initPeerLink();
        }

        /**
         * Constructor for existing Link
         */
        public RNSPeer(Link link) {
            this.peerLink = link;
            this.serverIdentity = this.peerLink.getRemoteIdentity();

            this.peerDestination = this.peerLink.getDestination();
            this.destinationHash = this.peerDestination.getHash();
            this.peerDestination.setProofStrategy(ProofStrategy.PROVE_ALL);

            setCreationTimestamp(System.currentTimeMillis());
            this.lastAccessTimestamp = null;
            this.isInitiator = true;

            this.peerLink.setLinkEstablishedCallback(this::linkEstablished);
            this.peerLink.setLinkClosedCallback(this::linkClosed);
            this.peerLink.setPacketCallback(this::linkPacketReceived);
        }

        public void initPeerLink() {
            this.peerDestination = new Destination(
                this.serverIdentity,
                Direction.OUT, 
                DestinationType.SINGLE,
                APP_NAME,
                "meshexample"
            );
            this.peerDestination.setProofStrategy(ProofStrategy.PROVE_ALL);

            setCreationTimestamp(System.currentTimeMillis());
            this.lastAccessTimestamp = null;
            this.isInitiator = true;

            this.peerLink = new Link(peerDestination);

            this.peerLink.setLinkEstablishedCallback(this::linkEstablished);
            this.peerLink.setLinkClosedCallback(this::linkClosed);
            this.peerLink.setPacketCallback(this::linkPacketReceived);
        }

        //public void initPeerBuffer(int receiveStreamId, int sendStreamId) {
        //    if (this.peerLink.getStatus() == ACTIVE) {
        //        var channel = this.peerLink.getChannel();
        //        Buffer.createBidirectionalBuffer(receiveStreamId, sendStreamId, channel, this::peerBufferReady);
        //    } else {
        //        log.info("cannot initiate buffer with peerLink status: {}", this.peerLink.getStatus());
        //    }
        //}

        @Synchronized
        public BufferedRWPair getOrInitPeerBuffer() {
            //Channel channel = this.peerLink.getChannel();
            //log.info("peer channel: {}", channel);
            if (nonNull(this.peerBuffer)) {
                log.info("peerBuffer exists: {}, link status: {}", this.peerBuffer, this.peerLink.getStatus());
                return this.peerBuffer;
            } else {
                var channel = this.peerLink.getChannel();
                log.info("creating buffer - peerLink status: {}, channel: {}", this.peerLink.getStatus(), channel);
                this.peerBuffer = Buffer.createBidirectionalBuffer(receiveStreamId, sendStreamId, channel, this::peerBufferReady);
            }
            return getPeerBuffer();
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
                log.info("shutdown - peerLink: {}, status: {}",
                    this.peerLink, this.peerLink.getStatus());
                if ((useBuffer) & nonNull(this.peerBuffer)) {
                    this.peerBuffer.close();
                    this.peerBuffer = null;
                }
                if (this.peerLink.getStatus() == ACTIVE) {
                    this.peerLink.teardown();
                }
                this.peerLink = null;
            }
        }

        public void linkEstablished(Link link) {
            link.setLinkClosedCallback(this::linkClosed);
            if (useBuffer) {
                var channel = this.peerLink.getChannel();
                this.peerBuffer = Buffer.createBidirectionalBuffer(receiveStreamId, sendStreamId, channel, this::peerBufferReady);
            }
            log.info("peerLink {} established (link: {}) with peer: hash - {}, link destination hash: {}", 
                this.peerLink, link, Hex.encodeHexString(this.destinationHash),
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
                    log.info("closing link: {}", this.peerLink.getDestination().getHexHash());
                    if (useBuffer) {
                        this.peerBuffer.close();
                        this.peerBuffer = null;
                    }
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
                    if (useBuffer) {
                        getOrInitPeerBuffer();
                    }
                }
            }
        }

        //// When the buffer has new data, process ist
        //// here, process = read it and write it to the terminal
        //public void peerBufferReady(Integer readyBytes) {
        //    var data = this.peerBuffer.read(readyBytes);
        //    var decodedData = new String(data);
        //    log.info("(initiator) Received data on the buffer: {}", decodedData);
        //    System.out.print("> ");
        //}

        /*
         * Callback from buffer when buffer has data available
         * 
         * :param readyBytes: The number of bytes ready to read
         */
        public void peerBufferReady(Integer readyBytes) {
            var data = this.peerBuffer.read(readyBytes);
            var decodedData = new String(data);

            log.info("Received data over the buffer: {}", decodedData);
            log.debug("server - latestClientLink status: {}", this.peerLink.getStatus());

            // process data. In this example: reply data back to client
            var replyText = "I received ** "+decodedData;
            byte[] replyData = replyText.getBytes();
            this.peerBuffer.write(replyData);
            this.peerBuffer.flush();
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
            if (nonNull(link)) {
                if (peerLink.getStatus() == ACTIVE) {
                    log.info("pinging remote: {}", link);
                    var data = "ping".getBytes(UTF_8);
                    link.setPacketCallback(this::linkPacketReceived);
                    Packet pingPacket = new Packet(link, data);
                    PacketReceipt packetReceipt = pingPacket.send();
                    // Note: is setTimeout needed (?), we want it to timeout with FAIL if remote peer is unreachable.
                    packetReceipt.setTimeout(3000L);
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
        Option o_buffer = Option.builder("b").longOpt("buffer")
                            .hasArg(false)
                            .required(false)
                            .desc("use buffer for transfer (instead of raw link)")
                            .build();
        options.addOption(o_buffer);
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

            if (cLine.hasOption("b")) {
                System.out.println("buffer mode - using Reticulum Buffer for data transfer");
                instance.setUseBuffer(true);
            }

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
