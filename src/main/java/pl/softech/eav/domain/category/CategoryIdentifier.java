package pl.softech.eav.domain.category;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import pl.softech.eav.domain.AbstractValueObject;
import pl.softech.eav.domain.TextMedium;

/**
 * @author ssledz
 */
@Embeddable
@Access(AccessType.FIELD)
public class CategoryIdentifier extends AbstractValueObject {

	public static final String IDENTIFIER_PROPERTY = "identifier";

	@TextMedium
	@Column(nullable = false)
	private String identifier;

	protected CategoryIdentifier() {
	}

	public CategoryIdentifier(String identifier) {
		this.identifier = checkNotNull(identifier, ARG_NOT_NULL_CHECK, "identifier");
	}

	public String getIdentifier() {
		return identifier;
	}

}