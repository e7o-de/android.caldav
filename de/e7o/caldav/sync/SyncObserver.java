package de.e7o.caldav.sync;

public interface SyncObserver {
	public void syncDone(String result);
	public void syncError(String result);
	public void syncState(String state);
}