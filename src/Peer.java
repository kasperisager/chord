// Networking
import networking.Host;

// General
import java.util.Deque;

// I/O
import java.io.Serializable;

// Remote Method Invocation
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The {@link Peer} interface describes a Chord node in a peer-to-peer network.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Chord_(peer-to-peer)">https://en.wikipedia.org/wiki/Chord_(peer-to-peer)</a>
 */
public interface Peer extends Serializable, Remote {
  /**
   * Get the {@link Key} of the {@link Peer}.
   *
   * @return The {@link Key} of the {@link Peer}.
   */
  Key key() throws RemoteException;

  /**
   * Get the successor of the {@link Peer}.
   *
   * @return The successor of the {@link Peer}.
   */
  Peer successor() throws RemoteException;

  /**
   * Get the list of successors of the {@link Peer}.
   *
   * @return The list of successors of the {@link Peer}.
   */
  Deque<Peer> successors() throws RemoteException;

  /**
   * Get the predecessor of the {@link Peer}.
   *
   * @return The predecessor of the {@link Peer}.
   */
  Peer predecessor() throws RemoteException;

  /**
   * Find the successor responsible for the given {@link Key}.
   *
   * @param key The {@link Key} to find the responsible successor for.
   * @return    The {@link Peer} responsible for the given {@link Key}.
   */
  Peer findSuccessor(final Key key) throws RemoteException;

  /**
   * Notify the current {@link Peer} about the existence of the specified {@link Peer}.
   *
   * @param peer The {@link Peer} to notify the current {@link Peer} about.
   */
  void notify(final Peer peer) throws RemoteException;

  /**
   * Retrieve the object with the specified {@link Key}.
   *
   * @param key The {@link Key} of the object to retrieve.
   * @param <T> The type of the object to retrieve.
   * @return    The object with the specified {@link Key} if found, otherwise
   *            null.
   */
  <T extends Serializable> T get(final Key key) throws RemoteException;

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
  <T extends Serializable> T put(final Key key, final Serializable object) throws RemoteException;

  /**
   * Offer a {@link Key} and an object to the {@link Peer}.
   *
   * @param key    The {@link Key} to offer.
   * @param object The object to offer.
   */
  void offer(final Key key, final Serializable object) throws RemoteException;
}
