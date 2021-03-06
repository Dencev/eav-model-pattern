/*
 * Copyright 2013 Sławomir Śledź <slawomir.sledz@sof-tech.pl>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.softech.eav.domain.object;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;

import pl.softech.eav.domain.AbstractEntity;
import pl.softech.eav.domain.TextMedium;
import pl.softech.eav.domain.attribute.Attribute;
import pl.softech.eav.domain.attribute.AttributeIdentifier;
import pl.softech.eav.domain.category.Category;
import pl.softech.eav.domain.relation.Relation;
import pl.softech.eav.domain.relation.RelationConfiguration;
import pl.softech.eav.domain.relation.RelationIdentifier;
import pl.softech.eav.domain.specification.ObjectMatchAttributeSpecification;
import pl.softech.eav.domain.specification.ValueMatchAttributeSpecification;
import pl.softech.eav.domain.value.AbstractValue;
import pl.softech.eav.domain.value.BooleanValue;
import pl.softech.eav.domain.value.DateValue;
import pl.softech.eav.domain.value.DictionaryEntryValue;
import pl.softech.eav.domain.value.DoubleValue;
import pl.softech.eav.domain.value.IntegerValue;
import pl.softech.eav.domain.value.ObjectValue;
import pl.softech.eav.domain.value.StringValue;
import pl.softech.eav.domain.value.ValueVisitor;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sławomir Śledź <slawomir.sledz@sof-tech.pl>
 * @since 1.0
 */
@Entity(name="pl.softech.eav.domain.object.MyObject")
@Table(name = "eav_my_object")
public class MyObject extends AbstractEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "object")
	private final Set<ObjectValue> values = Sets.newHashSet();

	@TextMedium
	@Column(nullable = false)
	private String name;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "owner")
	private final Set<Relation> relations = Sets.newHashSet();

	protected MyObject() {
	}

	public MyObject(Builder builder) {
		this(builder.category, builder.name);

		for (Pair<Attribute, ? extends AbstractValue<?>> p : builder.values) {
			addValue(p.getLeft(), p.getRight());
		}
		
		for(Pair<RelationConfiguration, MyObject> r : builder.relations) {
			addRelation(r.getLeft(), r.getRight());
		}

	}

	public MyObject(Category category, String name) {
		this.category = checkNotNull(category, ARG_NOT_NULL_CHECK, "category");
		this.name = checkNotNull(name, ARG_NOT_NULL_CHECK, "name");
	}

	public Category getCategory() {
		return category;
	}

	public ImmutableSet<ObjectValue> getValues() {
		return new ImmutableSet.Builder<ObjectValue>().addAll(values).build();
	}

	public ImmutableSet<ObjectValue> getValuesByAttribute(final AttributeIdentifier attributeIdentifier) {
		return new ImmutableSet.Builder<ObjectValue>().addAll(Iterables.filter(values, new Predicate<ObjectValue>() {

			@Override
			public boolean apply(ObjectValue input) {
				return input.getAttribute().getIdentifier().equals(attributeIdentifier);
			}
		})).build();
	}

	public ObjectValue getValueByAttribute(AttributeIdentifier attributeIdentifier) {
		return Iterables.getOnlyElement(getValuesByAttribute(attributeIdentifier), null);
	}

	public boolean hasValues(AttributeIdentifier attributeIdentifier) {
		return !getValuesByAttribute(attributeIdentifier).isEmpty();
	}

	public ImmutableSet<Relation> getRelations() {
		return new ImmutableSet.Builder<Relation>().addAll(relations).build();
	}

	public ImmutableSet<Relation> getRelationsByIdentifier(final RelationIdentifier identifier) {

		return new ImmutableSet.Builder<Relation>().addAll(Iterables.filter(relations, new Predicate<Relation>() {

			@Override
			public boolean apply(Relation input) {
				return input.getConfiguration().getIdentifier().equals(identifier);
			}
		})).build();

	}

	public Relation getRelationByIdentifier(RelationIdentifier identifier) {
		return Iterables.getOnlyElement(getRelationsByIdentifier(identifier), null);
	}

	public Relation addRelation(RelationConfiguration configurarion, MyObject target) {
		Relation r = new Relation(configurarion, this, target);
		relations.add(r);
		return r;
	}

	public <T extends AbstractValue<?>> ObjectValue updateValue(final Attribute attribute, T value) {

		checkNotNull(attribute);

		ObjectValue objValue = getValueByAttribute(attribute.getIdentifier());

		if (objValue != null) {
			values.remove(objValue);
		}

		if (value == null) {
			return null;
		}

		return addValue(attribute, value);
	}
	
	public Relation updateRelation(RelationConfiguration configurarion, MyObject target) {
		checkNotNull(configurarion);
		
		Relation relation = getRelationByIdentifier(configurarion.getIdentifier());
		
		if(relation != null) {
			relations.remove(relation);
		}
		
		if(target == null) {
			return null;
		}
		
		return addRelation(configurarion, target);
		
	}

	public <T extends AbstractValue<?>> ObjectValue addValue(final Attribute attribute, T value) {

		checkNotNull(attribute);
		checkNotNull(value);

		ObjectMatchAttributeSpecification catConstraint = new ObjectMatchAttributeSpecification();

		checkArgument(catConstraint.isSafisfiedBy(Pair.of(this, attribute)), String.format(
				"Object category %s doesn't match the Attribute category %s", category.getIdentifier().getIdentifier(), attribute
						.getCategory().getIdentifier().getIdentifier()));

		ValueMatchAttributeSpecification dataTypeConstraint = new ValueMatchAttributeSpecification();

		checkArgument(dataTypeConstraint.isSafisfiedBy(Pair.of(value, attribute)),
				String.format("Attribute %s doesn't match the value %s", attribute.toString(), value.toString()));

		final ObjectValue[] bag = new ObjectValue[1];

		ValueVisitor visitor = new ValueVisitor() {

			@Override
			public void visit(DateValue value) {
				bag[0] = new ObjectValue(attribute, MyObject.this, value);
			}

			@Override
			public void visit(DictionaryEntryValue value) {
				bag[0] = new ObjectValue(attribute, MyObject.this, value);
			}

			@Override
			public void visit(DoubleValue value) {
				bag[0] = new ObjectValue(attribute, MyObject.this, value);
			}

			@Override
			public void visit(IntegerValue value) {
				bag[0] = new ObjectValue(attribute, MyObject.this, value);
			}

			@Override
			public void visit(BooleanValue value) {
				bag[0] = new ObjectValue(attribute, MyObject.this, value);
			}

			@Override
			public void visit(StringValue value) {
				bag[0] = new ObjectValue(attribute, MyObject.this, value);
			}
		};

		value.accept(visitor);

		checkNotNull(bag[0]);

		values.add(bag[0]);

		return bag[0];
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		sb.appendSuper(super.toString());
		sb.append("name", name);
		sb.append("category", category);
		sb.append("values", values);
		sb.append("relations", relations);
		return sb.toString();
	}

	public static class Builder {

		private Category category;

		private String name;

		private final Collection<Pair<Attribute, ? extends AbstractValue<?>>> values = Lists.newLinkedList();
		
		private final Collection<Pair<RelationConfiguration, MyObject>> relations = Lists.newLinkedList();
		
		public Builder withCategory(Category category) {
			this.category = category;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public <T extends AbstractValue<?>> Builder add(Attribute attribute, T value) {
			values.add(Pair.of(attribute, value));
			return this;
		}
		
		public Builder add(RelationConfiguration relation, MyObject target) {
			relations.add(Pair.of(relation, target));
			return this;
		}

	}

}
