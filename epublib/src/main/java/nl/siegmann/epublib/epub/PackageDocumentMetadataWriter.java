package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Meta;

import org.rr.commons.utils.StringUtil;
import org.xmlpull.v1.XmlSerializer;

public class PackageDocumentMetadataWriter extends PackageDocumentBase {

	
	/**
	 * Writes the book's metadata.
	 *
	 * @param book
	 * @param serializer
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 * @
	 */
	public static void writeMetaData(Book book, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
		serializer.startTag(NAMESPACE_OPF, OPFTags.metadata);
		serializer.setPrefix(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE);
		serializer.setPrefix(PREFIX_OPF, NAMESPACE_OPF);
		
		writeIdentifiers(book.getMetadata().getIdentifiers(), serializer);
		writeSimpleMetdataElements(DCTags.title, book.getMetadata().getTitles(), serializer);
		writeSimpleMetdataElements(DCTags.subject, book.getMetadata().getSubjects(), serializer);
		writeSimpleMetdataElements(DCTags.description, book.getMetadata().getDescriptions(), serializer);
		writeSimpleMetdataElements(DCTags.publisher, book.getMetadata().getPublishers(), serializer);
		writeSimpleMetdataElements(DCTags.type, book.getMetadata().getTypes(), serializer);
		writeSimpleMetdataElements(DCTags.rights, book.getMetadata().getRights(), serializer);

		// write authors
		for(Author author: book.getMetadata().getAuthors()) {
			serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.creator);
			serializer.attribute(NAMESPACE_OPF, OPFAttributes.role, author.getRelator().getCode());
			serializer.attribute(NAMESPACE_OPF, OPFAttributes.file_as, author.toString());
			serializer.text((author.getFirstname() + " " + author.getLastname()).trim());
			serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.creator);
		}

		// write contributors
		for(Author author: book.getMetadata().getContributors()) {
			serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor);
			serializer.attribute(NAMESPACE_OPF, OPFAttributes.role, author.getRelator().getCode());
			serializer.attribute(NAMESPACE_OPF, OPFAttributes.file_as, author.toString());
			serializer.text((author.getFirstname() + " " + author.getLastname()).trim());
			serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor);
		}
		
		// write dates
		for (Date date: book.getMetadata().getDates()) {
			serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.date);
			if (date.getEvent() != null) {
				serializer.attribute(NAMESPACE_OPF, OPFAttributes.event, date.getEvent().toString());
			}
			serializer.text(date.getValue());
			serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.date);
		}

		// write language
		if(StringUtil.isNotEmpty(book.getMetadata().getLanguage())) {
			serializer.startTag(NAMESPACE_DUBLIN_CORE, "language");
			serializer.text(book.getMetadata().getLanguage());
			serializer.endTag(NAMESPACE_DUBLIN_CORE, "language");
		}
		
		//write name content meta tags
		Meta coverMeta = removeMetaByName(book.getMetadata().getOtherMeta(), OPFValues.meta_cover);
		if(book.getMetadata().getOtherMeta() != null) {
			final List<Meta> otherMeta = book.getMetadata().getOtherMeta();
			for(Meta meta: otherMeta) {
				serializer.startTag(NAMESPACE_OPF, "meta");
				serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.name, meta.getName());
				serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content, meta.getContent());
				serializer.endTag(NAMESPACE_OPF, "meta");
			}
		}

		// write other properties
		if(book.getMetadata().getOtherProperties() != null) {
			for(Map.Entry<QName, String> mapEntry: book.getMetadata().getOtherProperties().entrySet()) {
				serializer.startTag(mapEntry.getKey().getNamespaceURI(), mapEntry.getKey().getLocalPart());
				serializer.text(mapEntry.getValue());
				serializer.endTag(mapEntry.getKey().getNamespaceURI(), mapEntry.getKey().getLocalPart());
				
			}
		}

		// write coverimage
		if(book.getCoverImage() != null || coverMeta != null) {
			serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
			serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.name, OPFValues.meta_cover);
			if(book.getCoverImage() != null) {
				serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content, book.getCoverImage().getId());
			} else {
				serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content, coverMeta.getContent());
			}
			serializer.endTag(NAMESPACE_OPF, OPFTags.meta);
		}

		// write generator
		//Now handled with the otherMetadata
//		serializer.startTag(NAMESPACE_OPF, OPFTags.meta);
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.name, OPFValues.generator);
//		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, OPFAttributes.content, Constants.EPUBLIB_GENERATOR_NAME);
//		serializer.endTag(NAMESPACE_OPF, OPFTags.meta);
		
		serializer.endTag(NAMESPACE_OPF, OPFTags.metadata);
	}
	
	private static Meta removeMetaByName(List<Meta> meta, String name) {
		if(meta != null) {
			for (int i=0; i < meta.size(); i++) {
				Meta m = meta.get(i);
				if(m.getName().equals(name)) {
					return meta.remove(i);
				}
			}
			
		}
		return null;
	}
	
	private static void writeSimpleMetdataElements(String tagName, List<String> values, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
		for(String value: values) {
			if (StringUtil.isEmpty(value)) {
				continue;
			}
			serializer.startTag(NAMESPACE_DUBLIN_CORE, tagName);
			serializer.text(value);
			serializer.endTag(NAMESPACE_DUBLIN_CORE, tagName);
		}
	}

	
	/**
	 * Writes out the complete list of Identifiers to the package document.
	 * The first identifier for which the bookId is true is made the bookId identifier.
	 * If no identifier has bookId == true then the first bookId identifier is written as the primary.
	 *
	 * @param identifiers
	 * @param serializer
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	private static void writeIdentifiers(List<Identifier> identifiers, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException  {
		Identifier bookIdIdentifier = Identifier.getBookIdIdentifier(identifiers);
		if(bookIdIdentifier == null) {
			bookIdIdentifier = new Identifier();
			identifiers.add(bookIdIdentifier);
		}
		
		serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
		serializer.attribute(EpubWriter.EMPTY_NAMESPACE_PREFIX, DCAttributes.id, BOOK_ID_ID);
		if(bookIdIdentifier.getScheme() != null && !bookIdIdentifier.getScheme().isEmpty()) {
			serializer.attribute(NAMESPACE_OPF, OPFAttributes.scheme, bookIdIdentifier.getScheme());
		}
		serializer.text(bookIdIdentifier.getValue());
		serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);

		for(Identifier identifier: identifiers.subList(1, identifiers.size())) {
			if(identifier == bookIdIdentifier) {
				continue;
			}
			serializer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
			serializer.attribute(NAMESPACE_OPF, "scheme", identifier.getScheme());
			serializer.text(identifier.getValue());
			serializer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier);
		}
	}

}
