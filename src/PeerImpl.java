// Console
import console.Terminal;

// Networking
import networking.Channel;
import networking.Host;
import networking.Port;

// Remote
import remote.Proxy;

// Threading
import threading.Schedule;

// General
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

// I/O
import java.io.IOException;
import java.io.Serializable;

// Remote Method Invocation
import java.rmi.RemoteException;

// Concurrency
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@link PeerImpl} class is the implementation of a {@link Peer} in a
 * peer-to-peer network.
 *
 * @see <a href="https://pdos.csail.mit.edu/papers/ton:chord/paper-ton.pdf">https://pdos.csail.mit.edu/papers/ton:chord/paper-ton.pdf</a>
 * @see <a href="https://gist.github.com/thomaslee/b4b150d333ba45df3bdf">https://gist.github.com/thomaslee/b4b150d333ba45df3bdf</a>
 */
public final class PeerImpl extends Proxy<Peer> implements Peer {
  /**
   * Unique ID used for serialization.
   */
  private static final long serialVersionUID = 129178;

  /**
   * The interval between stabilization of the {@link PeerImpl Peer}.
   */
  private static final int STABILIZATION_INTERVAL = 4000;

  /**
   * The replication factor of the {@link PeerImpl Peer}.
   */
  private static final int REPLICATION_FACTOR = 2;

  /**
   * The amount of time to wait before declaring a {@link PeerImpl Peer} dead.
   */
  private static final int PEER_TIMEOUT = 500;

  /**
   * The {@link Key} of the {@link Peer}.
   */
  private final Key key;

  /**
   * The key/value pairs stored in the {@link Peer}.
   */
  private final Map<Key, Serializable> data = new HashMap<>();

  /**
   * Array of finger {@link PeerImpl Peers} that act as logarithmic short cuts
   * around the Chord ring.
   */
  private final Peer[] fingers = new Peer[Key.SIZE];

  /**
   * The list of successors of the {@link Peer}.
   */
  private volatile Deque<Peer> successors = new ArrayDeque<>();

  /**
   * The predecessor {@link Peer} in the network.
   */
  private volatile Peer predecessor;

  /**
   * Initialize a new {@link PeerImpl Peer} bound to specified {@link Host}.
   *
   * @param host The {@link Host} that the {@link PeerImpl Peer} binds to.
   */
  public PeerImpl(final Host host) throws IOException {
    super(host);

    // Use the hash of the host (address:port) as the key of the peer.
    this.key = new Key(host.hashCode());

    // Before the Peer has joined the network, it will point to itself.
    this.successor(this);

    // Schedule regular stabilization of the peer.
    new Schedule(() -> this.stabilize(), STABILIZATION_INTERVAL);
  }

  /**
   * Initialize a new {@link PeerImpl Peer} bound to specified {@link Host}
   * which will join the {@link PeerImpl Peer} at the given {@link Host}.
   *
   * @param host The {@link Host} that the {@link PeerImpl Peer} binds to.
   * @param peer A known {@link PeerImpl Peer} to join.
   */
  public PeerImpl(final Host host, final Peer known) throws IOException {
    this(host);

    if (known == null) {
      throw new IllegalArgumentException("A known peer must be specified");
    }

    this.join(known);
  }

  /**
   * Initialize a new {@link PeerImpl Peer} bound to specified {@link Host}
   * which will join the given {@link PeerImpl Peer} at the specified {@link Host}.
   *
   * @param host The {@link Host} that the {@link PeerImpl Peer} binds to.
   * @param peer The {@link Host} of a known {@link PeerImpl Peer} to join.
   */
  public PeerImpl(final Host host, final Host peer) throws IOException {
    this(host, (Peer) Proxy.connect(peer));
  }

  /**
   * Get the {@link Key} of the {@link PeerImpl Peer}.
   *
   * @return The {@link Key} of the {@link PeerImpl Peer}.
   */
  public Key key() throws RemoteException {
    return this.key;
  }

  /**
   * Get the successor of the {@link PeerImpl Peer}.
   *
   * @return The successor of the {@link PeerImpl Peer}.
   */
  public Peer successor() throws RemoteException {
    // Check if the immediate successor (the first finger) is still alive.
    if (!isAlive(this.fingers[0])) {
      // If the immediate successor is dead, find a replacement.
      synchronized (this.successors) {
        this.successor(this.successors.stream()
          // Skip the immediate successor as this is the one that died.
          .skip(1)
          // Get the first successor who's still alive.
          .filter(successor -> isAlive(successor)).findFirst()
          // If none was found, use the current peer.
          .orElse(this)
        );
      }

      // Reconsile the successor list now that a new successor has been found.
      this.reconsileSuccessors();
    }

    return this.fingers[0];
  }

  /**
   * Set the successor of the {@link PeerImpl Peer}.
   *
   * @param peer The successor of the {@link PeerImpl Peer}.
   */
  private void successor(final Peer peer) throws RemoteException {
    synchronized (this.fingers) {
      this.fingers[0] = peer;
    }
  }

  /**
   * Get the list of successors of the {@link PeerImpl Peer}.
   *
   * @return The list of successors of the {@link PeerImpl Peer}.
   */
  public Deque<Peer> successors() throws RemoteException {
    return this.successors;
  }

  /**
   * Get the predecessor of the {@link PeerImpl Peer}.
   *
   * @return The predecessor of the {@link PeerImpl Peer}.
   */
  public Peer predecessor() throws RemoteException {
    if (this.predecessor != null && !isAlive(this.predecessor)) {
      this.predecessor = null;
    }

    return this.predecessor;
  }

  /**
   * Set the predecessor of the {@link PeerImpl Peer}.
   *
   * @param peer The predecessor of the {@link PeerImpl Peer}.
   */
  private void predecessor(final Peer peer) throws RemoteException {
    this.predecessor = peer;
  }

  /**
   * Find the successor responsible for the given key.
   *
   * @param key The key to find the responsible successor for.
   * @return    The {@link Peer} responsible for the given key.
   */
  public Peer findSuccessor(final Key key) throws RemoteException {
    Peer successor = this.successor();

    if (key.isBetween(this.key, successor.key())) {
      return successor;
    }

    Peer closest = this.closest(key);

    // Return the current peer if it itself is closest to the key.
    if (closest == this) {
      return this;
    }
    // Otherwise, ask the closest peer to continue the search.
    else {
      return closest.findSuccessor(key);
    }
  }

  /**
   * Notify the current {@link PeerImpl Peer} about the existence of the
   * specified {@link Peer}.
   *
   * @param peer The {@link Peer} to notify the current {@link PeerImpl Peer}
   *             about.
   */
  public void notify(final Peer peer) throws RemoteException {
    if (peer == null) {
      throw new IllegalArgumentException("A peer must be specified");
    }

    Peer predecessor = this.predecessor();

    // If no predecessor currently exists, then the peer is as good as any.
    if (predecessor == null) {
      this.predecessor(peer);
    }
    // Bail out if the current peer notified itself of its existence, which can
    // happen when only a single peer exists in the network.
    else if (peer == this) {
      return;
    }
    // If the current peer doesn't have a predecessor or if the new peer is
    // better suited than the current predecessor, then use the new peer as the
    // predecessor instead.
    else if (peer.key().isBetween(predecessor.key(), this.key)) {
      this.predecessor(peer);
    }
  }

  /**
   * Retrieve the object with the specified {@link Key}.
   *
   * @param key The {@link Key} of the object to retrieve.
   * @param <T> The type of the object to retrieve.
   * @return    The object with the specified {@link Key} if found, otherwise
   *            null.
   */
  @SuppressWarnings("unchecked")
  public <T extends Serializable> T get(final Key key) throws RemoteException {
    // Find the peer responsible for the specified key.
    Peer responsible = this.findSuccessor(key);

    // If the current peer is responsible for the key, get it.
    if (this.key.equals(responsible.key())) {
      synchronized (this.data) {
        return (T) this.data.get(key);
      }
    }
    // Otherwise, ask the responsible peer to get the key.
    else {
      return (T) responsible.get(key);
    }
  }

  /**
   * Store the given object with the specified {@link Key}.
   *
   * @param key    The {@link Key} of the object to store.
   * @param object The object to store.
   * @param <T>    The type of the object already associated with the {@link Key}
   *               if any.
   * @return       The object previously associated with the {@link Key} if any,
   *               otherwise null.
   */
  @SuppressWarnings("unchecked")
  public <T extends Serializable> T put(final Key key, final Serializable object) throws RemoteException {
    // Find the peer responsible for the specified key.
    Peer responsible = this.findSuccessor(key);

    // If the current peer is responsible for the key, store the object.
    if (this.key.equals(responsible.key())) {
      synchronized (this.data) {
        return (T) this.data.put(key, object);
      }
    }
    // Otherwise, ask the responsible peer to store the object.
    else {
      return (T) responsible.put(key, object);
    }
  }

  /**
   * Offer a {@link Key} and an object to the {@link Peer}.
   *
   * @param key    The {@link Key} to offer.
   * @param object The object to offer.
   */
  public void offer(final Key key, final Serializable object) throws RemoteException {
    synchronized (this.data) {
      // If the key isn't already stored within the peer then store it.
      if (!this.data.containsKey(key)) {
        this.data.put(key, object);
      }
    }
  }

  /**
   * Find the {@link PeerImpl Peer} closest to the given {@link Key}.
   *
   * @param key The {@link Key} to find the {@link PeerImpl Peer} closest to.
   * @return    The {@link PeerImpl Peer} closest to the given {@link Key}.
   */
  private Peer closest(final Key key) throws RemoteException {
    // Initially assume that the current peer is closest to the key.
    Peer candidate = this;

    synchronized (this.fingers) {
      for (Peer peer: this.fingers) {
        // Move on to the next finger if this one is dead.
        if (!isAlive(peer)) {
          continue;
        }

        // If the key of the finger lies between that of the current key and the
        // given key, then the finger is a possible candidate for being closest
        // to the key.
        if (peer.key().isBetween(this.key, key)) {
          candidate = peer;
        }
      }
    }

    return candidate;
  }

  /**
   * Join the network through the specified {@link Peer}.
   *
   * @param peer The {@link Peer} to join the network through.
   */
  private void join(final Peer peer) throws RemoteException {
    if (peer == null) {
      throw new IllegalArgumentException("A peer must be specified");
    }

    // Join the known peer by setting it as the successor of the current peer.
    this.successor(peer.findSuccessor(this.key));
  }

  /**
   * Verify the immediate successor of the current {@link PeerImpl Peer}.
   */
  private void stabilize() throws RemoteException {
    Peer successor = this.successor();
    Peer candidate = successor.predecessor();

    // Is the successor of the current peer has a predecessor (candidate) than
    // lies between the current peer and its successor, then the candidate can
    // be used as the successor of the current peer instead.
    if (candidate != null && candidate.key().isBetween(this.key, successor.key())) {
      this.successor(candidate);
    }

    // Notify the successor of the current peer (possible the new candidate)
    // about its existence.
    this.successor().notify(this);

    // Fix the finger table of the peer.
    this.fixFingers();

    // Handoff any data keys that the peer is no longer responsible for.
    this.handoff();

    // Reconsile the list of backup successors.
    this.reconsileSuccessors();
  }

  /**
   * Fix the finger {@link Key Keys} of the {@link PeerImpl Peer}.
   */
  private void fixFingers() throws RemoteException {
    synchronized (this.fingers) {
      // Go through each of the fingers in the finger table, treating their
      // indices as bits of the peer keys. The immediate successor isn't touched
      // so the loop skips the first bit.
      for (byte bits = 1; bits < this.fingers.length; bits++) {
        // Set the finger as the successor of the peer key shifted the given
        // number of bits. As an example, key "0" shifted 0 bits is "1", 1 bit
        // is "2", 2 bits is "4", 3 bits is "8", and so forth. This ensures that
        // the fingers are placed with logarithmic distances around the chord
        // ring.
        this.fingers[bits] = this.findSuccessor(this.key.shift(bits));
      }
    }
  }

  /**
   * Hand off {@link Key Keys} and objects that the current peer is no longer
   * responsible for.
   */
  private void handoff() throws RemoteException {
    synchronized (this.data) {
      for (Key key: this.data.keySet()) {
        // Find the peer responsible for the data key.
        Peer responsible = this.findSuccessor(key);

        // If the current peer is no longer responsible for the key, then offer
        // it to the responsible peer.
        if (!this.key().equals(responsible.key())) {
          responsible.offer(key, this.data.remove(key));
        }
      }
    }
  }

  /**
   * Reconsile the successors of the {@link PeerImpl Peer}.
   */
  private void reconsileSuccessors() throws RemoteException {
    Peer successor = this.successor();

    // If the peer is its own successor then there's no point in reconciling the
    // list.
    if (successor == this) {
      return;
    }

    // Get the successor list from the successor of the peer.
    Deque<Peer> successors = successor.successors();

    // Add the immediate successor as the first item of the list.
    successors.addFirst(successor);

    // Trim the successor list to the replication factor.
    if (successors.size() > REPLICATION_FACTOR) {
      successors.removeLast();
    }

    this.successors = successors;
  }

  /**
   * Check if the specified {@link Peer} is still alive.
   *
   * @param peer The {@link Peer} to check.
   * @return     A boolean inidicating whether or not the {@link Peer} is still
   *             alive.
   */
  private static boolean isAlive(final Peer peer) {
    if (peer == null) {
      throw new IllegalArgumentException("A peer must be specified");
    }

    try {
      // Construct a future task for attempting to contact the peer. Since RMI
      // seemingly allows no control over timeouts, this rather hacky solution
      // is used instead.
      FutureTask<Key> future = new FutureTask<>(() -> {
        // Invoke a remote method on the peer to check if it responds.
        return peer.key();
      });

      // Run the future in a thread of its own.
      new Thread(future).start();

      // Wait for some time before declaring the peer dead. Upon timeout, an
      // exception will be thrown.
      future.get(PEER_TIMEOUT, TimeUnit.MILLISECONDS);

      // If it responded, then it's still alive.
      return true;
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      // If an exception is thrown, then the peer is dead.
      return false;
    }
  }

  /**
   * Start up a {@link PeerImpl Peer}.
   *
   * @param args Runtime arguments.
   */
  public static final void main(final String[] args) throws Exception {
    Peer peer;

    switch (args.length) {
      case 1:
        peer = new PeerImpl(new Host(args[0]));
        break;

      case 2:
        peer = new PeerImpl(new Host(args[0]), new Host(args[1]));
        break;

      default:
        throw new IllegalArgumentException("Expected arguments: <host> [<join>]");
    }

    Terminal terminal = new Terminal(System.in, System.out);

    terminal.listen(arguments -> {
      String command = arguments.read();

      switch (command) {
        case "get": {
          if (arguments.length() != 2) {
            throw new IllegalArgumentException("Usage: get <key>");
          }

          Key key = new Key(arguments.readInt());
          String value = peer.get(key);

          terminal.println("{" + key + ": " + (value != null ? value : "null") + "}");
          break;
        }

        case "put": {
          if (arguments.length() != 3) {
            throw new IllegalArgumentException("Usage: put <key> <value>");
          }

          Key key = new Key(arguments.readInt());
          String value = arguments.read();
          String old = peer.put(key, value);

          terminal.println("{" + key + ": " + (old != null ? old + " -> " : "") + value + "}");
          break;
        }

        case "key": {
          if (arguments.length() != 1) {
            throw new IllegalArgumentException("Usage: key");
          }

          terminal.println(peer.key());
          break;
        }

        case "successor": {
          if (arguments.length() != 2) {
            throw new IllegalArgumentException("Usage: successor <key>");
          }

          terminal.println(peer.findSuccessor(new Key(arguments.readInt())).key());
          break;
        }

        default:
          throw new IllegalArgumentException("Unknown command: " + command);
      }
    });
  }
}
