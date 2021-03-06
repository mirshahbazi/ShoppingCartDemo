package com.shoppingcartdemo.mode.shop;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shoppingcartdemo.bean.GoodsBean;
import com.shoppingcartdemo.bean.ShopCartBean;
import com.shoppingcartdemo.mode.IMode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiangcheng on 18/1/5.
 */

public class ShopMode implements IMode<ShopCartBean> {
    private static final String TAG = ShopMode.class.getSimpleName();
    private Context context;
    private double price;
    private List<ShopCartBean> select_list = new ArrayList<>();//传到结算页面的商品数据
    private List<ShopCartBean> allShopCarBean = new ArrayList<>();//传到结算页面的商品数据

    private ShopLoaderListener listener;

    public ShopMode(Context context, ShopLoaderListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void loadList() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open("shopcartdata.json")));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }

            String json = stringBuilder.toString();

            Gson gson = new Gson();
            List<ShopCartBean> list = gson.fromJson(json, new TypeToken<List<ShopCartBean>>() {
            }.getType());//对于不是类的情况，用这个参数给出
            listener.loadSuccess(list);
            allShopCarBean.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void numberReduce(int parent_position, int child_position) {
        ShopCartBean bean = allShopCarBean.get(parent_position);
        List<GoodsBean> goodsList = bean.getGoods();
        GoodsBean goodsBean = goodsList.get(child_position);
        String goods_num = goodsBean.getGoods_number();
        int goodsNum = Integer.parseInt(goods_num);
        boolean canReduce = false;
        if (goodsNum > 1) {
            canReduce = true;
        }
        GoodsBean selectGoodsBean = goodsNumChange(2, parent_position, child_position);
        Log.d(TAG, "goodsBean.number:" + goodsBean.getGoods_number());
        if (selectGoodsBean.isCheck() && canReduce) {
            price -= Double.parseDouble(selectGoodsBean.getGoods_price());
            Log.d(TAG, "price:" + price);
            listener.onNumberReduce(price, select_list);
        }
    }

    public void itemChildClick(int position) {
        ShopCartBean bean = allShopCarBean.get(position);
        int index = isContainsShopBean(select_list, bean);
        if (index != -1) {
            select_list.remove(index);
        }

        boolean isSelected;
        boolean checkAll;
        if (bean.isCheck()) {
            isSelected = false;
        } else {
            isSelected = true;
        }

        //保存店铺点击状态
        bean.setCheck(isSelected);
        //通知全选CheckBox的选择状态
        if (allSelect() == allShopCarBean.size()) {
            checkAll = true;
        } else {
            checkAll = false;
        }
        if (isSelected) {
            for (int i = 0; i < bean.getGoods().size(); i++) {
                if (!bean.getGoods().get(i).isCheck()) {
                    bean.getGoods().get(i).setCheck(true);
                    price += Double.parseDouble(bean.getGoods().get(i).getGoods_number()) * Double.parseDouble(bean.getGoods().get(i).getGoods_price());
                }
            }
            select_list.add(bean);
        } else {
            // 解决点击取消选择商品时，
            // 店铺全选按钮取消选择状态，不会不变成全不选
            if (allChildSelect(position) == bean.getGoods().size()) {
                for (int i = 0; i < bean.getGoods().size(); i++) {
                    if (bean.getGoods().get(i).isCheck()) {
                        bean.getGoods().get(i).setCheck(false);
                        price -= Double.parseDouble(bean.getGoods().get(i).getGoods_number()) * Double.parseDouble(bean.getGoods().get(i).getGoods_price());
                    }
                }
                select_list.remove(bean);
            }
        }
        listener.onItemChildClick(price, checkAll, select_list, position);
    }

    //计算店铺的选择数量
    private int allSelect() {
        int sum = 0;
        for (int i = 0; i < allShopCarBean.size(); i++) {
            if (allShopCarBean.get(i).isCheck()) {
                sum++;
            }
        }

        return sum;
    }

    //计算每个店铺商品的选择数量
    private int allChildSelect(int position) {
        int sum = 0;
        for (int i = 0; i < allShopCarBean.get(position).getGoods().size(); i++) {
            if (allShopCarBean.get(position).getGoods().get(i).isCheck()) {
                sum++;
            }
        }
        return sum;
    }

    public void childClick(int parent_position, int child_position) {
        ShopCartBean bean = allShopCarBean.get(parent_position);
        ShopCartBean selectBean = new ShopCartBean();
        selectBean.clearGoods(bean, select_list);

        List<GoodsBean> goodsList = bean.getGoods();
        GoodsBean goodsBean = goodsList.get(child_position);
        boolean isSelected;
        boolean checkAll;
        if (goodsBean.isCheck()) {
            isSelected = false;
            price -= Double.parseDouble(goodsBean.getGoods_number()) * Double.parseDouble(goodsBean.getGoods_price());
            selectBean.getGoods().remove(goodsBean);
        } else {
            isSelected = true;
            price += Double.parseDouble(goodsBean.getGoods_number()) * Double.parseDouble(goodsBean.getGoods_price());
            selectBean.getGoods().add(goodsBean);
        }
        //保存商品点击状态
        goodsBean.setCheck(isSelected);
        //通知店铺选择的状态
        if (allChildSelect(parent_position) == goodsList.size()) {
            bean.setCheck(true);
            selectBean.setCheck(true);
        } else {
            bean.setCheck(false);
            selectBean.setCheck(false);
        }
        int index = isContainsShopBean(select_list, selectBean);
        if (index != -1) {
            select_list.remove(index);
        }
        select_list.add(selectBean);

        //通知全选CheckBox的选择状态
        if (allSelect() == allShopCarBean.size()) {
            checkAll = true;
        } else {
            checkAll = false;
        }
        listener.onItemChildClick(price, checkAll, select_list, parent_position);
    }

    private int isContainsShopBean(List<ShopCartBean> existShopBeanList, ShopCartBean shopCartBean) {
        for (int i = 0; i < existShopBeanList.size(); i++) {
            ShopCartBean selectBean = existShopBeanList.get(i);
            Log.d(TAG, "selectBean.getSupplier_id" + selectBean.getSupplier_id());
            Log.d(TAG, "shopCartBean.getSupplier_id" + shopCartBean.getSupplier_id());
            if (selectBean.getSupplier_id().equals(shopCartBean.getSupplier_id())) {
                return i;
            }
        }
        return -1;
    }

    public void selectAll() {
        price = 0;
        select_list.clear();
        for (int i = 0; i < allShopCarBean.size(); i++) {
            ShopCartBean shopCartBean = allShopCarBean.get(i);

            //选择店铺
            if (!shopCartBean.isCheck()) {
                shopCartBean.setCheck(true);
            }
            for (int j = 0; j < shopCartBean.getGoods().size(); j++) {
                //选择店铺的商品
                if (!shopCartBean.getGoods().get(j).isCheck()) {
                    shopCartBean.getGoods().get(j).setCheck(true);
                    Log.d(TAG, "数量:" + shopCartBean.getGoods().get(j).getGoods_number());
                }
                price += Double.parseDouble(shopCartBean.getGoods().get(j).getGoods_number()) * Double.parseDouble(shopCartBean.getGoods().get(j).getGoods_price());
            }
            select_list.add(shopCartBean);
        }
        listener.onSelctAll(price, select_list);
    }

    public void unSelectAll() {
        if (allSelect() == allShopCarBean.size()) {
            for (int i = 0; i < allShopCarBean.size(); i++) {
                ShopCartBean shopCartBean = allShopCarBean.get(i);

                if (shopCartBean.isCheck()) {
                    shopCartBean.setCheck(false);
                }
                for (int j = 0; j < shopCartBean.getGoods().size(); j++) {
                    if (shopCartBean.getGoods().get(j).isCheck()) {
                        shopCartBean.getGoods().get(j).setCheck(false);
                    }
                }
            }
            select_list.clear();
            price = 0;
            listener.onUnSelectAll(price, select_list);
        }
    }

    public void numberAdd(int parent_position, int child_position) {
        GoodsBean goodsBean = goodsNumChange(1, parent_position, child_position);
        if (goodsBean.isCheck()) {
            price += Double.parseDouble(goodsBean.getGoods_price());
            listener.onNumberAdd(price, select_list);
        }
    }

    //商品数量的增减
    private GoodsBean goodsNumChange(int type, int parent_position, int child_position) {
        ShopCartBean bean = allShopCarBean.get(parent_position);

        List<GoodsBean> goodsList = bean.getGoods();
        GoodsBean goodsBean = goodsList.get(child_position);
        String goods_num = goodsBean.getGoods_number();
        int goodsNum = Integer.parseInt(goods_num);

        if (type == 1) {
            goodsNum = goodsNum + 1;
        } else {

            if (goodsNum > 1) {
                goodsNum = goodsNum - 1;
            }
        }

        goodsBean.setGoods_number(String.valueOf(goodsNum));
        ShopCartBean selectBean = new ShopCartBean();
        selectBean.clearGoods(bean, select_list);
        int index = isContainsShopBean(select_list, selectBean);
        if (index != -1) {
            select_list.remove(index);
        }
        select_list.add(selectBean);
        listener.onNumberChange(parent_position);
        return goodsBean;
    }
}
