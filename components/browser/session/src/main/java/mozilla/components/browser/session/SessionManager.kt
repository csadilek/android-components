/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session

import mozilla.components.browser.session.engine.EngineObserver
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.utils.observer.Observable
import mozilla.components.support.utils.observer.ObserverRegistry

/**
 * This class provides access to a centralized registry of all active sessions.
 */
class SessionManager(
    private val engine: Engine
) : Observable<SessionManager.Observer> by registry {
    private val values = mutableListOf<Session>()
    private var selectedIndex: Int = NO_SELECTION

    /**
     * Returns the number of values.
     */
    val size: Int
        get() = synchronized(values) { values.size }

    /**
     * Gets the currently selected session. Only one session can be selected.
     */
    val selectedSession: Session
        get() = synchronized(values) {
            if (selectedIndex == NO_SELECTION) {
                throw IllegalStateException("No selected session")
            }
            values[selectedIndex]
        }

    /**
     * Gets all sessions.
     */
    val sessions: List<Session>
        get() = synchronized(values) { values.toList() }

    /**
     * Adds the provided session.
     */
    fun add(session: Session, selected: Boolean = false, engineSession: EngineSession? = null) = synchronized(values) {
        values.add(session)

        engineSession?.let { link(session, it) }

        notifyObservers { onSessionAdded(session) }

        if (selected || selectedIndex == NO_SELECTION) {
            select(session)
        }
    }

    fun getEngineSession(session: Session = selectedSession) = session.engineSessionHolder.engineSession

    fun getOrCreateEngineSession(session: Session = selectedSession): EngineSession {
        val enginSession = getEngineSession(session)
        if (enginSession != null) {
            return enginSession
        }

        val newEngineSession = engine.createSession()
        link(session, newEngineSession)
        return newEngineSession
    }

    private fun link(session: Session, engineSession: EngineSession) {
        unlink(session)

        session.engineSessionHolder.let { holder ->
            holder.engineSession = engineSession
            holder.engineObserver = EngineObserver(session).also { observer ->
                engineSession.register(observer)
            }
            engineSession.loadUrl(session.url)
        }
    }

    fun unlink(session: Session) {
        session.engineSessionHolder.engineObserver?.let { observer ->
            session.engineSessionHolder.engineSession?.unregister(observer)
            session.engineSessionHolder.engineSession?.close()
            session.engineSessionHolder.engineSession = null
        }
    }

    /**
     * Removes the provided session. If no session is provided then the selected session is removed.
     */
    fun remove(session: Session = selectedSession) = synchronized(values) {
        val indexToRemove = values.indexOf(session)
        if (indexToRemove == -1) {
            return
        }

        values.removeAt(indexToRemove)

        unlink(session)

        notifyObservers { onSessionRemoved(session) }

        // Recalculate selection
        val newSelectedIndex = when {
            // All items have been removed
            values.size == 0 -> NO_SELECTION

            // Something on the left of our selection has been removed, we need to move the index to the left
            indexToRemove < selectedIndex -> selectedIndex - 1

            // The selection is out of bounds. Let's selected the previous item.
            selectedIndex == values.size -> selectedIndex - 1

            // We can keep the selected index.
            else -> selectedIndex
        }

        if (newSelectedIndex != selectedIndex) {
            selectedIndex = newSelectedIndex
            if (selectedIndex != NO_SELECTION) {
                notifyObservers { onSessionSelected(selectedSession) }
            }
        }
    }

    /**
     * Removes all sessions.
     */
    fun removeAll() = synchronized(values) {
        values.forEach { session ->
            unlink(session)
        }

        values.clear()

        selectedIndex = NO_SELECTION

        notifyObservers { onAllSessionsRemoved() }
    }

    /**
     * Marks the given session as selected.
     */
    fun select(session: Session) = synchronized(values) {
        val index = values.indexOf(session)

        if (index == -1) {
            throw IllegalArgumentException("Value to select is not in list")
        }

        selectedIndex = index

        notifyObservers { onSessionSelected(session) }
    }

    /**
     * Finds and returns the session with the given id. Returns null if no matching session could be
     * found.
     */
    fun findSessionById(id: String): Session? = values.find { session -> session.id == id }

    companion object {
        const val NO_SELECTION = -1
    }

    /**
     * Interface to be implemented by classes that want to observe the session manager.
     */
    interface Observer {
        /**
         * The selection has changed and the given session is now the selected session.
         */
        fun onSessionSelected(session: Session) = Unit

        /**
         * The given session has been added.
         */
        fun onSessionAdded(session: Session) = Unit

        /**
         * The given session has been removed.
         */
        fun onSessionRemoved(session: Session) = Unit

        /**
         * All sessions have been removed. Note that this will callback will be invoked whenever
         * <code>removeAll()</code> has been called on the <code>SessionManager</code>. This
         * callback will NOT be invoked when just the last session has been removed by calling
         * <code>remove()</code> on the <code>SessionManager</code>.
         */
        fun onAllSessionsRemoved() = Unit
    }
}

private val registry = ObserverRegistry<SessionManager.Observer>()
