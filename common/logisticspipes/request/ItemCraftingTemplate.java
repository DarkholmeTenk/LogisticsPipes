/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.request;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.LogisticsExtraPromise;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

public class ItemCraftingTemplate implements Comparable<ItemCraftingTemplate>, ICraftingTemplate {

	protected ItemIdentifierStack _result;
	protected ICraftItems _crafter;

	protected ArrayList<Pair<IResource, IAdditionalTargetInformation>> _required = new ArrayList<Pair<IResource, IAdditionalTargetInformation>>(9);

	protected ArrayList<ItemIdentifierStack> _byproduct = new ArrayList<ItemIdentifierStack>(9);

	private final int priority;

	public ItemCraftingTemplate(ItemIdentifierStack result, ICraftItems crafter, int priority) {
		_result = result;
		_crafter = crafter;
		this.priority = priority;
	}

	public void addRequirement(IResource requirement, IAdditionalTargetInformation info) {
		_required.add(new Pair<IResource, IAdditionalTargetInformation>(requirement, info));
	}

	public void addByproduct(ItemIdentifierStack stack) {
		for (ItemIdentifierStack i : _byproduct) {
			if (i.getItem().equals(stack.getItem())) {
				i.setStackSize(i.getStackSize() + stack.getStackSize());
				return;
			}
		}
		_byproduct.add(stack);
	}

	@Override
	public LogisticsPromise generatePromise(int nResultSets) {
		return new LogisticsPromise(_result.getItem(), _result.getStackSize() * nResultSets, _crafter, ResourceType.CRAFTING);
	}

	//TODO: refactor so that other classes don't reach through the template to the crafter.
	// needed to get the crafter todo, in order to sort
	@Override
	public ICraftItems getCrafter() {
		return _crafter;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public int compareTo(ItemCraftingTemplate o) {
		int c = priority - o.priority;
		if (c == 0) {
			c = _result.compareTo(o._result);
		}
		if (c == 0) {
			c = _crafter.compareTo(o._crafter);
		}
		return c;
	}

	@Override
	public boolean canCraft(IResource type) {
		if (type instanceof ItemResource) {
			return ((ItemResource) type).getItem().equals(_result.getItem());
		} else if (type instanceof DictResource) {
			return ((DictResource) type).matches(_result.getItem());
		}
		return false;
	}

	@Override
	public int getResultStackSize() {
		return _result.getStackSize();
	}

	@Override
	public IResource getResultItem() {
		return new ItemResource(_result, null);
	}

	@Override
	public List<IExtraPromise> getByproducts(int workSets) {
		List<IExtraPromise> list = new ArrayList<IExtraPromise>();
		for (ItemIdentifierStack stack : _byproduct) {
			list.add(new LogisticsExtraPromise(stack.getItem(), stack.getStackSize() * workSets, getCrafter(), false));
		}
		return list;
	}

	@Override
	public List<Pair<IResource, IAdditionalTargetInformation>> getComponents(int nCraftingSetsNeeded) {
		List<Pair<IResource, IAdditionalTargetInformation>> stacks = new ArrayList<Pair<IResource, IAdditionalTargetInformation>>(_required.size());

		// for each thing needed to satisfy this promise
		for (Pair<IResource, IAdditionalTargetInformation> stack : _required) {
			Pair<IResource, IAdditionalTargetInformation> pair = new Pair<IResource, IAdditionalTargetInformation>(stack.getValue1().clone(nCraftingSetsNeeded), stack.getValue2());
			stacks.add(pair);
		}
		return stacks;
	}
}
