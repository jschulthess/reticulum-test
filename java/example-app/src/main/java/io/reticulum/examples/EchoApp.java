package io.reticulum.examples;

import io.reticulum.Reticulum;
import io.reticulum.Transport;
import io.reticulum.destination.Destination;
import io.reticulum.destination.DestinationType;
import io.reticulum.destination.Direction;
import io.reticulum.destination.ProofStrategy;
import io.reticulum.identity.Identity;
import io.reticulum.packet.Packet;
import io.reticulum.packet.PacketType;
import io.reticulum.packet.PacketReceipt;
import io.reticulum.packet.PacketReceiptStatus;
import io.reticulum.transport.AnnounceHandler;
import static io.reticulum.identity.IdentityKnownDestination.recall;
import io.reticulum.utils.IdentityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
//import java.util.Random;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Scanner;

import org.apache.commons.codec.binary.Hex;

@Slf4j
public class EchoApp {
    private static final String APP_NAME = "echo_example";
    Reticulum reticulum;
    Identity identity;
    Transport transport;
    public Destination destination1, destination2;

    static final  String defaultConfigPath = new String(".reticulum"); // if empty will look in Reticulums default paths
    
    /** Server */
    private void server_setup() {
        try {
            reticulum = new Reticulum(".reticulum");
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }
        identity = new Identity();

        destination1 = new Destination(
            identity,
            Direction.IN,
            DestinationType.SINGLE,
            APP_NAME,
            "announcesample",
            "fruits"
        );
        log.info("destination1 hash: "+destination1.getHexHash());
        destination1.setProofStrategy(ProofStrategy.PROVE_ALL);
        destination1.setPacketCallback(this::server_callback);

        // create a custom announce handler instance
        var announceHandler = new ExampleAnnounceHandler();

        // register announce handler
        transport = Transport.getInstance();
        transport.registerAnnounceHandler(announceHandler);
        log.debug("announce handlers: {}", transport.getAnnounceHandlers());
    }

    public void server_run() {
        announceLoop(destination1);
    }

    public void announceLoop(Destination destination) {
        log.info("Echo server {} running, hit enter to manually send an announce (Ctrl-C to quit)", Hex.encodeHexString(destination.getHash()));
        String inData;
        while (true) {
            Scanner scan = new Scanner( System.in );
            inData = scan.nextLine();
            destination.announce();
            log.info("Sent announce from {} ({})", Hex.encodeHexString(destination.getHash()), destination.getName());
        }
        //Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
        //    () -> {
        //        var appDataString = new String("echo_app");
        //        d1.announce(appDataString.getBytes(UTF_8));
        //         
        //        log.debug("Sent announce from {} ({}), data: {}", Hex.encodeHexString(d1.getHash()), d1.getName(), d1.getDefaultAppData());
        //    }, 30,30, TimeUnit.SECONDS
        //);
    }

    public void server_callback (byte[] message, Packet packet) {
        var receptionStats = new String("");
        if (reticulum.isConnectedToSharedInstance()) {
            log.info("shared instance - no stats");
        } else {
            var reception_rssi = packet.getRssi();
            System.out.println("RSSI "+reception_rssi+" dBm");
        }
    }

    private class ExampleAnnounceHandler implements AnnounceHandler {
        @Override
        public String getAspectFilter() {
            log.info("getAspectFilter called.");
            //return APP_NAME;
            return null;
        }
        
        @Override
        public void receivedAnnounce(byte[] destinationHash, Identity announcedIdentity, byte[] appData) {
            log.info("Received an announce from {}", Hex.encodeHexString(destinationHash));
            
            if (appData != null) {
                log.info("The announce contained the following app data: {}", new String(appData));
            }
        }
    }

    /** Client */
    private void client_setup(byte[] destinationHash, Long timeout) {
        try {
            reticulum = new Reticulum(".reticulum");
        } catch (IOException e) {
            log.error("unable to create Reticulum network", e);
        }
        log.info("Echo client ready, hit enter to send echo request to {} (Ctrl-C to quit)", destinationHash);

        String inData;
        Identity serverIdentity;
        Destination requestDestination;
        Scanner scan = new Scanner( System.in );

        try {
            while (true) {
                inData = scan.nextLine();
                serverIdentity = recall(destinationHash);

                requestDestination = new Destination(
                    serverIdentity,
                    Direction.OUT,
                    DestinationType.SINGLE,
                    APP_NAME,
                    "echo",
                    "request"
                );
                Packet echoRequest = new Packet(requestDestination, IdentityUtils.getRandomHash(), PacketType.DATA);
                PacketReceipt packetReceipt = echoRequest.send();

                packetReceipt.setTimeout(timeout);
                packetReceipt.setTimeoutCallback(this::packetTimeoutCallback);

                packetReceipt.setDeliveryCallback(this::packetDeliveredCallback);
            }
        } catch (Exception e) {
            scan.close();
        }
    }

    public void packetDeliveredCallback(PacketReceipt receipt) {
        if (receipt.getStatus() == PacketReceiptStatus.DELIVERED) {
            log.info("Valid reply received from {}", receipt.getDestination().getHash());
        }
    }

    public void packetTimeoutCallback(PacketReceipt receipt) {
        if (receipt.getStatus() == PacketReceiptStatus.FAILED) {
            log.info("packet {} timed out", Hex.encodeHexString(receipt.getHash()));
        }
    }

    /** Main */
    public static void main(String[] args) throws IOException {
        // main to run application directly
        var instance = new EchoApp();
        if ("s".equals(args[0])) {
            instance.server_setup();
            instance.server_run();
        } else if ("c".equals(args[0])) {
            if (isBlank(args[1])) {
                System.out.println("Usage: run_echo.sh c <destination_hash>");
            } else {
                instance.client_setup(args[1].getBytes(UTF_8), 20*1000L); // timeout: 20s
            }
        } else {
            System.out.println("Usage (server): run_echo.sh s");
            System.out.println("Usage (client): run_echo.sh c <destination_hash>");
            System.out.println("'c': client or 's': server (listening mode)");
        }
    }

}