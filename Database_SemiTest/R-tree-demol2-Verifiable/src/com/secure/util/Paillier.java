package com.secure.util;


/**
 * 密钥生成：
 * 1、随机选择两个大质数p和q满足gcd（pq,(p-1)(q-1)）=1。 这个属性是保证两个质数长度相等。
 * 2、计算 n = pq和λ= lcm (p - 1,q-1)。
 * 3、选择随机整数g使得gcd(L(g^lambda % n^2) , n) = 1,满足g属于n^2;
 * 4、公钥为（N，g）
 * 5、私钥为lambda。
 * :加密
 * 选择随机数r满足
 * 计算密文
 * 其中m为加密信息
 *
 * 解密：
 * m = D(c,lambda) = ( L(c^lambda%n^2)/L(g^lambda%n^2) )%n;
 * 其中L(u) = (u-1)/n;
 */

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Paillier {

    //p,q是两个随机的质数， lambda = lcm(p-1, q-1);
    private BigInteger p  = new BigInteger("64262511718702417097639704319728232165640431457295456883338479893071095012199");
            private BigInteger q = new BigInteger("74347156378613716944633542986088642648647529202320220135613892539670034047813");
            public static BigInteger lambda = new BigInteger("2388867504016432571775639645197458448424735371892436632471526088339933867218084948906624018606256989625292528519404131013469466387065932456820134727605388") ;

    // n = p*q
    public static BigInteger n = new BigInteger("4777735008032865143551279290394916896849470743784873264943052176679867734436308507481345353346556252497890873913622549987598548451150817286073010584270787");

    // nsquare就是n的平方
    public static BigInteger nsquare = new BigInteger("22826751806982801957776865135471031401384326354300046161625289756402680368491929658012259259533799620986387200716747810852634658882129491518336929450689336595269830720465031904858718040449525134789319869047321592224003797888505466678248096172530402192953040880085138272681735359145193314263725690092541599369");
    /**
     * 随机选取一个整数 g,g属于小于n的平方中的整数集,且 g 满足:g的lambda次方对n的平方求模后减一后再除与n，
     * 最后再将其与n求最大公约数，且最大公约数等于一。
     * a random integer in Z*_{n^2} where gcd (L(g^lambda mod nsquare), n) = 1.
     */
    private static BigInteger g = new BigInteger("86");
    //bitLength 模量
    private static int bitLength = 512;

    /**
     * Constructs an instance of the Paillier cryptosystem.
     *
     * @param bitLengthVal
     *            number of bits of modulus 模量
     * @param certainty
     *            The probability that the new BigInteger represents a prime
     *            number will exceed (1 - 2^(-certainty)). The execution time of
     *            this constructor is proportional to the value of this
     *            parameter.
     *带参的构造方法
     */
    public Paillier(int bitLengthVal, int certainty) {
        KeyGeneration(bitLengthVal, certainty);
    }

    /**
     * Constructs an instance of the Paillier cryptosystem with 512 bits of
     * modulus and at least 1-2^(-64) certainty of primes generation.
     * 构造方法
     */
    public Paillier() {
//        KeyGeneration(512, 64);
    }

    /**
     * 产生公钥【N,g】       私钥lamada
     * @param bitLengthVal
     *            number of bits of modulus.
     * @param certainty
     *            certainty - 调用方允许的不确定性的度量。
     *            新的 BigInteger 表示素数的概率超出 (1 - 1/2certainty)。
     *            此构造方法的执行时间与此参数的值是成比例的。
     */
    public void KeyGeneration(int bitLengthVal, int certainty) {
        bitLength = bitLengthVal;
        //构造两个随机生成的正 大质数，长度可能为bitLength/2，它可能是一个具有指定 bitLength 的素数
        p = new BigInteger(bitLength / 2, certainty, new Random());
        q = new BigInteger(bitLength / 2, certainty, new Random());

        //n = p*q;
        n = p.multiply(q);
        //nsquare = n*n;
        nsquare = n.multiply(n);
        //随机生成一个0~100的整数g
        g = new BigInteger( String.valueOf( (int) (  Math.random()*100 ) ));

        //lamada=lcm(p-1,q-1),即lamada是p-1,q-1的最小公倍数
        //lamada=((p-1)*(q-1)) / gcd(p-1,q-1);
        lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))  //(p-1)*(q-1)
                .divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)));
        //检验g是否符合公式的要求， gcd (L(g^lambda mod nsquare), n) = 1.
        if (g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).gcd(n).intValue() != 1) {
            System.out.println("g is not good. Choose g again.");
            System.exit(1);
        }
    }

    /**
     * @param m 明文m
     * @param r 随机的一个整数r
     * @return 返回密文
     * 加密
     */
    public BigInteger Encryption(BigInteger m, BigInteger r) {
        //c = (g^m)*(r^n)modnsquare
        return g.modPow(m, nsquare).multiply(r.modPow(n, nsquare)).mod(nsquare);
    }

    public static BigInteger Encryption(BigInteger m) {
        //构造一个随机生成的 BigInteger，它是在 0 到 (2numBits - 1)（包括）范围内均匀分布的值。
        BigInteger r = new BigInteger(bitLength, new Random());
        return g.modPow(m, nsquare).multiply(r.modPow(n, nsquare)).mod(nsquare);

    }

    /**
     * 利用私钥lamada对密文c进行解密返回明文m
     * 公式：m = ( L((c^lambda) mod nsquare) / L((g^lambda) mod nsquare) ) mod n
     */
    public static BigInteger Decryption(BigInteger c) {
        BigInteger u1 = c.modPow(lambda, nsquare);
        BigInteger u2 = g.modPow(lambda, nsquare);
        return (u1.subtract(BigInteger.ONE).divide(n)).multiply(u2.subtract(BigInteger.ONE).divide(n).modInverse(n)).mod(n);
    }

  

    /**
     * 两个密文相乘
     * @param c1
     * @param c2
     */
    public static BigInteger SM(BigInteger c1, BigInteger c2){
        BigInteger r_a = new BigInteger(10, new Random());
        BigInteger r_b = new BigInteger(10, new Random());

        BigInteger a_ = c1.multiply(Encryption(r_a)).mod(nsquare);
        BigInteger b_ = c2.multiply(Encryption(r_b)).mod(nsquare);
        
        BigInteger h_a = Decryption(a_);
        BigInteger h_b = Decryption(b_);
        BigInteger h = h_a.multiply(h_b);
        BigInteger h_ = Encryption(h);

        BigInteger s = h_.multiply(c1.modPow(n.subtract(r_b),nsquare)).mod(nsquare);
        BigInteger s_ = s.multiply(c2.modPow(n.subtract(r_a),nsquare)).mod(nsquare);

        return s_.multiply(Encryption(r_a.multiply(r_b)).
                           modPow(n.subtract(new BigInteger("1")),nsquare)).mod(nsquare);
    }

    /**
     * 计算
     * @param x
     * @param y
     * @return
     */
    public static BigInteger SSED(BigInteger x, BigInteger y){

        return null;
    }
    /**
     * 比较两个密文的大小
     * @param x
     * @param y
     * @return x\<=y,返回[1];x>y, 返回[0]
     */
    public static BigInteger IntegerComp(BigInteger x, BigInteger y){
        BigInteger res;
        BigInteger z ;
        BigInteger u;

        int F = (int) (Math.random()*5+1);

        BigInteger a = x.modPow(new BigInteger("2"), nsquare);

        BigInteger b = Encryption(new BigInteger("1")).
                multiply(y.modPow(new BigInteger("2"), Paillier.nsquare)).mod(nsquare);


        if (F%2==1){
            z = a.multiply(b.modPow(Paillier.n.subtract(new BigInteger("1")),nsquare))
                    .mod(nsquare);
        }else{
            z = b.multiply(a.modPow(Paillier.n.subtract(new BigInteger("1")),nsquare))
                    .mod(nsquare);
        }




        BigInteger d = Decryption(z);

        if (d.compareTo(n.divide(new BigInteger("2")))==1 ){
            u = Encryption(new BigInteger("1"));
        }else {
            u = Encryption(new BigInteger("0"));
        }




        if (F%2==1){
            res = u;
        }else{
            res = Encryption(new BigInteger("1")).
                    multiply(u.modPow(n.subtract(new BigInteger("1")),nsquare)).mod(nsquare);
        }
        return res;
    }

    /**
     * 比较两个矢量密文的大小
     * @param a
     * @param b
     * @return 
     */
    public static ArrayList<BigInteger> VectorComp(List<BigInteger> a, List<BigInteger> b){

        ArrayList<BigInteger> a_ = new ArrayList<BigInteger>();
        ArrayList<BigInteger> b_ = new ArrayList<BigInteger>();
        ArrayList<BigInteger> alpha  = new ArrayList<BigInteger>();
        ArrayList<BigInteger> bata  = new ArrayList<BigInteger>();
        ArrayList<BigInteger> u = new ArrayList<BigInteger>();
        ArrayList<BigInteger> res  = new ArrayList<BigInteger>();


        int F = (int) (Math.random()*5+1);
        for(int i = 0; i<a.size();i++){
             a_.add(a.get(i).modPow(new BigInteger("2"), nsquare));

             b_.add(Encryption(new BigInteger("1")).
                    multiply(b.get(i).modPow(new BigInteger("2"), Paillier.nsquare)).mod(nsquare));
        }

        for(int i = 0; i<a.size();i++){
            if (F%2==1){
                alpha.add(a_.get(i).multiply(b_.get(i).modPow(Paillier.n.subtract(new BigInteger("1")),nsquare))
                        .mod(nsquare));
            }else{
                alpha.add(b_.get(i).multiply(a_.get(i).modPow(Paillier.n.subtract(new BigInteger("1")),nsquare))
                        .mod(nsquare));
            }

        }


        for(int i = 0; i<a.size();i++){
            bata.add(Decryption(alpha.get(i)));
        }

        for(int i = 0; i<a.size();i++){
            if (bata.get(i).compareTo(n.divide(new BigInteger("2")))==1 ){
                u.add(Encryption(new BigInteger("1")));
            }else {
                u.add(Encryption(new BigInteger("0")));
            }
        }


        for(int i = 0; i<a.size();i++){
            if (F%2==1){
                res.add(u.get(i)) ;
            }else{
                res.add(Encryption(new BigInteger("1")).
                        multiply(u.get(i).modPow(n.subtract(new BigInteger("1")),nsquare)).mod(nsquare));
            }
        }

        return res;
    }

    public static BigInteger SMIN(BigInteger a, BigInteger b){
        BigInteger res;
        BigInteger out = IntegerComp(a,b);
        BigInteger tmp1 =  Decryption(out);

        BigInteger fout = Encryption(new BigInteger("1")).multiply
                              (out.modPow(n.subtract(new BigInteger("1")),nsquare)).mod(nsquare);
        BigInteger tmp2 =  Decryption(fout);
        res = SM(a,out).multiply(SM(b,fout)).mod(nsquare);
        return res;
    }

    public static BigInteger ESDOM(List<BigInteger> a, List<BigInteger> b){
        ArrayList<BigInteger> c = VectorComp(a,b);
        BigInteger delta = c.get(0);
        for(int i = 1; i<c.size();i++){
            delta = SM(delta,c.get(i));
        }

        BigInteger s_a = a.get(0);
        for(int i = 1;i<a.size();i++){
            s_a = s_a.multiply(a.get(i));
        }

        BigInteger s_b = b.get(0);
        for(int i = 1;i<b.size();i++){
            s_b = s_b.multiply(b.get(i));
        }

        BigInteger u1 = IntegerComp(s_a,s_b);
        BigInteger u2 = IntegerComp(s_b,s_a);

        BigInteger f = u1.multiply(u2).multiply(SM(u1,u2).
                    modPow(n.subtract(new BigInteger("2")),nsquare)).mod(nsquare);

        return SM(delta,f);
    }


//    public static BigInteger SDOM(ArrayList<BigInteger> a, ArrayList<BigInteger> b){
//        ArrayList<BigInteger> c = new ArrayList<>();
//        Client client = new Client();
//
//
//        Socket s = client.init();
//        IntegerCompClient intClient = new IntegerCompClient();
//        for(int i = 0; i<a.size();i++){
//
//            BigInteger z = intClient.IntegerComp01(a.get(i),b.get(i));
//            client.sendMsg(z.toString());
//            String u =  client.receive();
//
//            c.add(intClient.IntegerComp02(new BigInteger(u)));
//        }
//        client.choseAll(s);
//        BigInteger delta = c.get(0);
//        for(int i = 1; i<c.size();i++){
//            delta = SM(delta,c.get(i));
//        }
//
//        BigInteger s_a = a.get(0);
//        for(int i = 1;i<a.size();i++){
//            s_a = s_a.multiply(a.get(i));
//        }
//
//        BigInteger s_b = b.get(0);
//        for(int i = 1;i<b.size();i++){
//            s_b = s_b.multiply(b.get(i));
//        }
//
//        BigInteger u1 = IntegerComp(s_a,s_b);
//        BigInteger u2 = IntegerComp(s_b,s_a);
//
//        BigInteger f = u1.multiply(u2).multiply(SM(u1,u2).
//                modPow(n.subtract(new BigInteger("2")),nsquare)).mod(nsquare);
//
//        return SM(delta,f);
//    }

    public static void main(String[] str) {
        //实例化一个不用传参的对象，用默认的数据
        Paillier paillier = new Paillier();
        // 实例化两个数据对象m1,m2，进行加密
        BigInteger m1 = new BigInteger("999999999");
        BigInteger m2 = new BigInteger("13");

        //加密
        BigInteger c1 = paillier.Encryption(m1);
        BigInteger c2 = paillier.Encryption(m2);
//
//        //输出加密后的结果
//        System.out.println("m1加密后为: "+c1);
//        System.out.println("m2加密后为: "+c2);
//        BigInteger c1 = new BigInteger("8184017101348481267815185318934872350411449153616099151960487637934899500661602129164732662708549271644666168457242463667714056037431193113269930546277175099865789364858182961040700179112712082910297769175325379464837058195063237112897825723092253067943656936077140123097097606462767730343476925944949287647");
//        BigInteger c2 = new BigInteger("4062324079024730769026294196825920842941338274972500410875029569787671940581727037279155795879178880649636776835542824967979214515961907345346550655832017016650358987587901970068409126509313602623015890221624099111459299639005079948169100490781938363956249701828283649965521219917751598545261573979677795547");
////        (,),Gigest is 267fb75ee7689a55d985228709e1f875; (7436584043,7436049416),Gigest is 2360157a4d8e55fe93eb70f6b556b3a7;
//        OPE o = new OPE();
//        System.out.println(o.decrypt(new BigInteger("7436584043")));
//        System.out.println(o.decrypt(new BigInteger("7436049416")));
//        try {
//            System.out.println(Digest.afterMD5(c1.toString()+c2.toString()));
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }

        //输出解密后的结果
        System.out.println("m1加密之后进行解密的结果= "+paillier.Decryption(c1).toString());
        System.out.println("m2加密之后进行解密的结果= "+paillier.Decryption(c2).toString());


        // 测试同态性     D(E(m1)*E(m2) mod n^2) = (m1 + m2) mod n

//        // m1+m2,求明文数值的和
//        BigInteger sum_m1m2 = m1.add(m2).mod(paillier.n);
//        System.out.println("明文的和 : " + sum_m1m2.toString());
//        // c1+c2，求密文数值的乘
//        BigInteger product_c1c2 = c1.multiply(c2).mod(paillier.nsquare);
//        System.out.println("密文的和: " + product_c1c2.toString());
//        System.out.println("解密后的 和: " + paillier.Decryption(product_c1c2).toString());
//
//        System.out.println("**************** 测试同态性 ->   D(E(m1)^m2 mod n^2) = (m1*m2) mod n**************");
//        // 测试同态性 ->   D(E(m1)^m2 mod n^2) = (m1*m2) mod n
//        // m1*m2,求明文数值的乘
//        BigInteger prod_m1m2 = m1.multiply(m2).mod(paillier.n);
//        System.out.println("明文的乘积: " + prod_m1m2.toString());
//        // c1的m2次方，再mod paillier.nsquare
//        BigInteger expo_c1m2 = c1.modPow(m2, paillier.nsquare);
//        System.out.println("密文的结果: " + expo_c1m2.toString());
//        System.out.println("解密后的结果: " + paillier.Decryption(expo_c1m2).toString());
//
//        System.out.println("****************测试密文相乘**************");
//        BigInteger SMc1c2 = paillier.SM(c1,c2);
//        System.out.println("密文相乘的结果: " + SMc1c2.toString());
//        System.out.println("解密后的结果: " + paillier.Decryption(SMc1c2).toString());
//
        System.out.println("****************测试密文大小**************");
        BigInteger cmpc1c2 = paillier.IntegerComp(c1,c2);
        System.out.println("密文相乘的结果: " + cmpc1c2.toString());
        System.out.println("解密后的结果: " + paillier.Decryption(cmpc1c2).toString());

        System.out.println("****************比较矢量密文我大小**************");
        BigInteger v1 = new BigInteger("10");
        BigInteger v2 = new BigInteger("900");
        BigInteger v3 = new BigInteger("222");
        BigInteger v4 = new BigInteger("223");

        //加密
        BigInteger cv1 = paillier.Encryption(v1);
        BigInteger cv2 = paillier.Encryption(v2);
        BigInteger cv3 = paillier.Encryption(v3);
        BigInteger cv4 = paillier.Encryption(v4);
        ArrayList<BigInteger> cv = new ArrayList<BigInteger>();
        cv.add(cv1);
        cv.add(cv2);
        cv.add(cv3);
        cv.add(cv4);

        BigInteger s1 = new BigInteger("10");
        BigInteger s2 = new BigInteger("99");
        BigInteger s3 = new BigInteger("255");
        BigInteger s4 = new BigInteger("415");

        //加密
        BigInteger cs1 = paillier.Encryption(s1);
        BigInteger cs2 = paillier.Encryption(s2);
        BigInteger cs3 = paillier.Encryption(s3);
        BigInteger cs4 = paillier.Encryption(s4);
        ArrayList<BigInteger> cs = new ArrayList<BigInteger>();
        cs.add(cs1);
        cs.add(cs2);
        cs.add(cs3);
        cs.add(cs4);

        long startTime=System.currentTimeMillis();

//        ArrayList<BigInteger> compv1v2 = paillier.VectorComp(cv,cs);
//        for(int i=0;i<3;i++){
//            System.out.println("密文"+i+"属性相乘的结果: " + compv1v2.get(i).toString());
//            System.out.println(i+"属性解密后的结果: " + paillier.Decryption(compv1v2.get(i)).toString());
//        }
        BigInteger esdom = Paillier.ESDOM(cv, cs);
        System.out.println("cv和cs支配关系："+ Paillier.Decryption(esdom));
        long endTime=System.currentTimeMillis();
        System.out.println("SDMOM运行时间： "+(endTime-startTime)+"ms");

    }
}