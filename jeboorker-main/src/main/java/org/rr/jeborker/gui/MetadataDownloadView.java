package org.rr.jeborker.gui;

import static org.rr.commons.utils.BooleanUtils.not;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FilenameUtils;
import org.rr.commons.collection.TransformValueList;
import org.rr.commons.log.LoggerFactory;
import org.rr.commons.mufs.IResourceHandler;
import org.rr.commons.swing.components.JRScrollPane;
import org.rr.commons.swing.components.JRTable;
import org.rr.commons.swing.components.container.ShadowPanel;
import org.rr.commons.utils.StringUtil;
import org.rr.jeborker.db.item.EbookPropertyItem;
import org.rr.jeborker.gui.cell.MetadataDownloadTableCellEditor;
import org.rr.jeborker.gui.cell.MetadataDownloadTableCellRenderer;
import org.rr.jeborker.gui.model.MetadataDownloadModel;
import org.rr.jeborker.gui.resources.ImageResourceBundle;
import org.rr.jeborker.metadata.IMetadataReader;
import org.rr.jeborker.metadata.IMetadataReader.COMMON_METADATA_TYPES;
import org.rr.jeborker.metadata.download.MetadataDownloadEntry;
import org.rr.jeborker.metadata.download.MetadataDownloadProviderFactory;
import org.rr.jeborker.metadata.download.MetadataDownloader;
import org.rr.jeborker.metadata.MetadataHandlerFactory;

class MetadataDownloadView extends AbstractDialogView {

	private static final long serialVersionUID = -8695330707780422570L;

	private static final int ABORT_BUTTON_INDEX = 0;

	private static final int OK_BUTTON_INDEX = 1;

	public static final int ACTION_RESULT_OK = 0;

	private int actionResult = -1;

	private boolean hasCoverWriterSupport = true;

	private Map<COMMON_METADATA_TYPES, List<Entry<JCheckBox, String>>> textValues;

	private byte[] coverImage;

	private MetadataDownloadController controller;

	private JPanel bottomPanel;

	private final ActionListener abortAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			controller.close();
		}
	};

	private final ActionListener okAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			MetadataDownloadTableCellEditor editor = (MetadataDownloadTableCellEditor) table.stopEdit();

			actionResult = ACTION_RESULT_OK;
			if(editor != null) {
				textValues = editor.getEditingValues();
				if(editor.isCoverImageChecked()) {
					MetadataDownloadEntry editorMetadataDownloadEntry = editor.getEditorMetadataDownloadEntry();
					coverImage = editorMetadataDownloadEntry.getCoverImage();
				}
			} else {
				textValues = new HashMap<COMMON_METADATA_TYPES, List<Entry<JCheckBox, String>>>();
			}

			controller.close();
		}
	};

	private final ActionListener searchAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final String selectedItem = (String) searchProviderCombobox.getSelectedItem();
			final MetadataDownloader downloader = MetadataDownloadProviderFactory.getDownloader(selectedItem, selectedEbookPropertyItems);
			final MetadataDownloadModel model = new MetadataDownloadModel(downloader, searchTextField.getText());
			final ShadowPanel shadowPanel = new ShadowPanel();

			setGlassPane(shadowPanel);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						shadowPanel.setVisible(true);
						model.loadSearchResult();

						//new renderer and editor instances for releasing cached resources and avoid get cached values
						//from the old search.
						table.setDefaultRenderer(MetadataDownloadEntry.class, new MetadataDownloadTableCellRenderer(hasCoverWriterSupport));
						table.setDefaultEditor(MetadataDownloadEntry.class, new MetadataDownloadTableCellEditor(new MetadataDownloadTableCellRenderer(hasCoverWriterSupport)));

						table.setModel(model);
					} finally {
						shadowPanel.setVisible(false);
					}
				}
			}).start();
		}
	};

	private final List<EbookPropertyItem> selectedEbookPropertyItems = MainController.getController().getSelectedEbookPropertyItems();
	
	private JRScrollPane scrollPane;

	private JRTable table;

	private JPanel searchPanel;

	private JTextField searchTextField;

	private JButton searchButton;

	private JLabel lblSearch;

	private JComboBox<String> searchProviderCombobox;
	
	public MetadataDownloadView(MetadataDownloadController controller, JFrame mainWindow) {
		super(mainWindow);
		setModal(true);
		this.controller = controller;
		initialize();
	}

	protected void initialize() {
		super.initialize();
		hasCoverWriterSupport = hasCoverWriterSupport(selectedEbookPropertyItems);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);

		searchPanel = new JPanel();
		GridBagConstraints gbc_searchPanel = new GridBagConstraints();
		gbc_searchPanel.insets = new Insets(5, 3, 5, 3);
		gbc_searchPanel.fill = GridBagConstraints.BOTH;
		gbc_searchPanel.gridx = 0;
		gbc_searchPanel.gridy = 0;
		getContentPane().add(searchPanel, gbc_searchPanel);
		GridBagLayout gbl_searchPanel = new GridBagLayout();
		gbl_searchPanel.columnWidths = new int[]{120, 0, 114, 27, 0};
		gbl_searchPanel.rowHeights = new int[]{25, 0};
		gbl_searchPanel.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_searchPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		searchPanel.setLayout(gbl_searchPanel);

		searchProviderCombobox = new JComboBox<String>(new DefaultComboBoxModel<>(MetadataDownloadProviderFactory.getDownloaderNames().toArray(new String[0])));
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets(0, 0, 0, 5);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 0;
		searchPanel.add(searchProviderCombobox, gbc_comboBox);

		lblSearch = new JLabel(Bundle.getString("MetadataDownloadView.label.search") + ":");
		GridBagConstraints gbc_lblSucheingabe = new GridBagConstraints();
		gbc_lblSucheingabe.insets = new Insets(0, 3, 0, 5);
		gbc_lblSucheingabe.anchor = GridBagConstraints.EAST;
		gbc_lblSucheingabe.gridx = 1;
		gbc_lblSucheingabe.gridy = 0;
		searchPanel.add(lblSearch, gbc_lblSucheingabe);

		searchTextField = new JTextField();
		GridBagConstraints gbc_searchTextField = new GridBagConstraints();
		gbc_searchTextField.fill = GridBagConstraints.BOTH;
		gbc_searchTextField.anchor = GridBagConstraints.WEST;
		gbc_searchTextField.insets = new Insets(0, 0, 0, 5);
		gbc_searchTextField.gridx = 2;
		gbc_searchTextField.gridy = 0;
		searchPanel.add(searchTextField, gbc_searchTextField);
		searchTextField.setColumns(10);
		searchTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					searchAction.actionPerformed(null);
				}
			}
		});
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				searchTextField.selectAll();
				searchTextField.requestFocus();
			}
		});

		if(!selectedEbookPropertyItems.isEmpty()) {
			String searchPhrase = (StringUtil.toString(selectedEbookPropertyItems.get(0).getAuthor()) + " " + StringUtil.toString(selectedEbookPropertyItems.get(0).getTitle())).trim();
			if(StringUtil.isNotEmpty(searchPhrase)) {
				searchTextField.setText(searchPhrase.trim());
			} else {
				String fileName = FilenameUtils.removeExtension(selectedEbookPropertyItems.get(0).getFileName());
				searchTextField.setText(fileName);
			}
		}

		searchButton = new JButton(ImageResourceBundle.getResourceAsImageIcon("play_16.png"));
		searchButton.setPreferredSize(new Dimension(27, 27));
		GridBagConstraints gbc_searchButton = new GridBagConstraints();
		gbc_searchButton.anchor = GridBagConstraints.NORTHEAST;
		gbc_searchButton.gridx = 3;
		gbc_searchButton.gridy = 0;
		searchPanel.add(searchButton, gbc_searchButton);
		searchButton.addActionListener(searchAction);

		scrollPane = new JRScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 3, 5, 3);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		getContentPane().add(scrollPane, gbc_scrollPane);

		table = new JRTable();
		table.setTableHeader(null);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setStopEditOnSelectionChange(true);
		scrollPane.setViewportView(table);

		//bottom OK and abort button
		bottomPanel = createBottomButtonPanel();
		GridBagConstraints gbc_panel2 = new GridBagConstraints();
		gbc_panel2.insets = new Insets(5, 3, 5, 3);
		gbc_panel2.fill = GridBagConstraints.BOTH;
		gbc_panel2.gridx = 0;
		gbc_panel2.gridy = 2;
		getContentPane().add(bottomPanel, gbc_panel2);
	}

	public int getActionResult() {
		return this.actionResult;
	}

	/**
	 * Test all selected resources for cover writer support.
	 * @param selectedEbookPropertyItems Selected items to be tested.
	 * @return <code>true</code> if all selected items have cover writer support or <code>false</code> if one
	 *     or more have not.
	 */
	private boolean hasCoverWriterSupport(final List<EbookPropertyItem> selectedEbookPropertyItems) {
		for(EbookPropertyItem selectedEbookPropertyItem : selectedEbookPropertyItems) {
			IResourceHandler resourceHandler = selectedEbookPropertyItem.getResourceHandler();
			boolean hasCoverWriterSupport = MetadataHandlerFactory.hasCoverWriterSupport(resourceHandler);
			if(!hasCoverWriterSupport) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the downloaded metadata string value for the given type.
	 * @return A list with the downloaded values for the given type. Each list entry
	 *     is a {@link Entry} with the checkbox boolean value as <code>key</code> and the
	 *     text with the <code>value</code>.
	 */
	public List<Entry<Boolean, String>> getValues(IMetadataReader.COMMON_METADATA_TYPES type) {
		List<Entry<JCheckBox, String>> list = textValues.get(type);
		if(list != null) {
			return new TransformValueList<Map.Entry<JCheckBox, String>, Map.Entry<Boolean,String>>(list) {

				@Override
				public Map.Entry<Boolean,String> transform(final Map.Entry<JCheckBox, String> source) {
					return new Map.Entry<Boolean, String>() {

						@Override
						public Boolean getKey() {
							JCheckBox checkbox = source.getKey();
							if(checkbox == null || not(checkbox.isSelected())) {
								return Boolean.FALSE;
							}
							return Boolean.TRUE;
						}

						@Override
						public String getValue() {
							return source.getValue();
						}

						@Override
						public String setValue(String value) {
							return null;
						}
					};
				}
			};
		}
		return Collections.<Entry<Boolean, String>> emptyList();
	}

	/**
	 * Get the cover image from the downloader.
	 */
	public byte[] getCoverImage() {
		return coverImage;
	}

	/**
	 * get the selected index of the search provider combobox.
	 */
	int getSearchProviderIndex() {
		return this.searchProviderCombobox.getSelectedIndex();
	}

	/**
	 * Set the given index to the search provider combobox.
	 */
	void setSearchProviderIndex(int index) {
		try  {
			this.searchProviderCombobox.setSelectedIndex(index);
		} catch(Exception e) {
			LoggerFactory.getLogger().log(Level.WARNING, "Failed to set search provider " + index, e);
		}
	}

	@Override
	protected int getBottomButtonCount() {
		return 2;
	}

	@Override
	protected ActionListener getBottomButtonAction(int index) {
		switch(index) {
			case ABORT_BUTTON_INDEX: return abortAction;
			case OK_BUTTON_INDEX: return okAction;
		}

		return null;
	}

	@Override
	protected String getBottomButtonLabel(int index) {
		switch(index) {
			case 0: return Bundle.getString("MetadataDownloadView.Abort");
			case 1: return Bundle.getString("MetadataDownloadView.OK");
		}
		return null;
	}

	@Override
	protected Dimension getDefaultDialogSize() {
		return new Dimension(700, 500);
	}

	@Override
	protected String getDialogTitle() {
		return Bundle.getString("MetadataDownloadView.title");
	}

	@Override
	protected Dimension getDialogMinimumSize() {
		return new Dimension(350, 250);
	}

	@Override
	protected boolean isDefaultButtonAction(int idx) {
		return idx == OK_BUTTON_INDEX;
	}

	@Override
	protected boolean isAbortAction(int idx) {
		return idx == ABORT_BUTTON_INDEX;
	}

}
