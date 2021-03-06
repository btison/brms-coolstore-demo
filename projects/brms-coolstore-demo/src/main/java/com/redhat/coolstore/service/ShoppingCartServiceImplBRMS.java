package com.redhat.coolstore.service;

import java.io.Serializable;

import javax.ejb.Stateful;
import javax.inject.Inject;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;

import com.redhat.coolstore.factmodel.PromoEvent;
import com.redhat.coolstore.model.Promotion;
import com.redhat.coolstore.model.ShoppingCart;
import com.redhat.coolstore.model.ShoppingCartItem;
import com.redhat.coolstore.util.BRMSUtil;

@Stateful
public class ShoppingCartServiceImplBRMS implements ShoppingCartService, Serializable {

	private static final long serialVersionUID = 6821952169434330759L;

	@Inject
	private BRMSUtil brmsUtil; 
	
	@Inject
	private PromoService promoService; 
	
	public ShoppingCartServiceImplBRMS() {
		
	}
		
	public void priceShoppingCart(ShoppingCart sc) {
						
		if ( sc != null ) {
						
			com.redhat.coolstore.factmodel.ShoppingCart factShoppingCart = new com.redhat.coolstore.factmodel.ShoppingCart();
			
			StatefulKnowledgeSession ksession = null;
			
			try {
			
				//if at least one shopping cart item exist
				if ( sc.getShoppingCartItemList().size() > 0 ) {
				
					ksession = brmsUtil.getStatefulSession();
					
					WorkingMemoryEntryPoint promoStream = ksession.getWorkingMemoryEntryPoint("Promo Stream");
					
					for (Promotion promo : promoService.getPromotions()) {
																	
						PromoEvent pv = new PromoEvent(promo.getItemId(), promo.getPercentOff());
						
						promoStream.insert(pv);
						
					}
																	
					ksession.insert(factShoppingCart);
					
					for (ShoppingCartItem sci : sc.getShoppingCartItemList()) {
						
						com.redhat.coolstore.factmodel.ShoppingCartItem factShoppingCartItem = new com.redhat.coolstore.factmodel.ShoppingCartItem();
						factShoppingCartItem.setItemId(sci.getProduct().getItemId());
						factShoppingCartItem.setName(sci.getProduct().getName());
						factShoppingCartItem.setPrice(sci.getProduct().getPrice());
						factShoppingCartItem.setQuantity(sci.getQuanity());
						factShoppingCartItem.setShoppingCart(factShoppingCart);
						
						ksession.insert(factShoppingCartItem);
						
					}
					
					ksession.startProcess("com.redhat.coolstore.PriceProcess");
					
					ksession.fireAllRules();
				
				}
				
				sc.setCartItemTotal(factShoppingCart.getCartItemTotal());
				sc.setCartItemPromoSavings(factShoppingCart.getCartItemPromoSavings());
				sc.setShippingTotal(factShoppingCart.getShippingTotal());
				sc.setShippingPromoSavings(factShoppingCart.getShippingPromoSavings());
				sc.setCartTotal(factShoppingCart.getCartTotal());
											
			} finally {
				
				if ( ksession != null ) {
					
					ksession.dispose();
					
				}
			}
		}
		
	}
	
}
