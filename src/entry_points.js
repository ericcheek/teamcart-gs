/**
 * Fetch 
 *
 * @return Table of product data with specified columns
 * @customfunction
 */
function getAmazonProductData() {
    return teamcartgs.core.get_amazon_product_data.apply(null, arguments);
}
