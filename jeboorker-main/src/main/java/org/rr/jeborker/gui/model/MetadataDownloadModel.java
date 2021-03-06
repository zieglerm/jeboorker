package org.rr.jeborker.gui.model;

import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.rr.commons.utils.ThreadUtils;
import org.rr.commons.utils.ThreadUtils.RunnableImpl;
import org.rr.jeborker.db.item.EbookPropertyItem;
import org.rr.jeborker.metadata.download.MetadataDownloadEntry;
import org.rr.jeborker.metadata.download.MetadataDownloader;

public class MetadataDownloadModel extends AbstractTableModel {

	private static final long serialVersionUID = -8746789528889474186L;

	private MetadataDownloader downloader;
	
	private String searchPhrase;
	
	private List<MetadataDownloadEntry> searchEntries;
	
	public MetadataDownloadModel(MetadataDownloader downloader, String searchPhrase) {
		this.downloader = downloader;
		this.searchPhrase = searchPhrase;
	}

	/**
	 * Invokes the {@link MetadataDownloader} and provide it's entries with this {@link MetadataDownloadModel}
	 * instance. This method blocks as long as all data for the model is loaded.
	 *
	 * This Method isn't be invoked automatically by the {@link MetadataDownloadModel} instance.
	 */
	public void loadSearchResult() {
		searchEntries = Collections.synchronizedList(this.downloader.search(this.searchPhrase));
		ThreadUtils.loopAndWait(searchEntries, new RunnableImpl<MetadataDownloadEntry, Void>() {

			@Override
			public Void run(MetadataDownloadEntry searchEntry) {
				searchEntry.getThumbnailImageBytes(); //lazy loading
				searchEntry.getDescription(); //lazy loading
				return null;
			}}, 10);
	}
	
	@Override
	public int getRowCount() {
		if(searchEntries != null) {
			return searchEntries.size();
		}
		return 0;
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return MetadataDownloadEntry.class.getName();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return MetadataDownloadEntry.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return searchEntries.get(rowIndex);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		searchEntries.set(rowIndex, (MetadataDownloadEntry) aValue);
	}

}
