import com.github.wangdong20.kotlinscriptcompiler.parser.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.type.BasicType;
import com.github.wangdong20.kotlinscriptcompiler.parser.type.Type;
import com.github.wangdong20.kotlinscriptcompiler.token.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private static void assertParses(final Exp expected,
                                    final Token... tokens) throws ParseException {
        assertEquals(expected, (new Parser(tokens)).parseToplevelExp());
    } // assertParses

    private static void assertExpectedException(final Exp expected, final Token... tokens) {
        assertThrows(ParseException.class,
                ()->{
                    assertEquals(expected, (new Parser(tokens)).parseToplevelExp());
                });
    }

    @Test
    public void emptyDoesNotParse() {
        assertExpectedException(null);
    }

    @Test
    public void integerParses() throws ParseException {
        assertParses(new IntExp(123), new IntToken(123));
    }

    @Test
    public void variableParses() throws ParseException {
        assertParses(new VariableExp("foo"), new VariableToken("foo"));
    }

    @Test
    public void parensParse() throws ParseException {
        assertParses(new VariableExp("foo"),
                BracketsToken.TK_LPAREN,
                new VariableToken("foo"),
                BracketsToken.TK_RPAREN);
    }

    @Test
    public void ifElseParses() throws ParseException {
        assertParses(new IfExp(new IntExp(1),
                        new IntExp(2),
                        new IntExp(3)),
                KeywordToken.TK_IF,
                BracketsToken.TK_LPAREN,
                new IntToken(1),
                BracketsToken.TK_RPAREN,
                new IntToken(2),
                KeywordToken.TK_ELSE,
                new IntToken(3));
    }

    @Test
    public void ifParses() throws ParseException {
        assertParses(new IfExp(new IntExp(1),
                        new IntExp(2),
                        null),
                KeywordToken.TK_IF,
                BracketsToken.TK_LPAREN,
                new IntToken(1),
                BracketsToken.TK_RPAREN,
                new IntToken(2));
    }

    @Test
    // ++i
    public void selfIncreasePreorderParses() throws ParseException {
        assertParses(new SelfOperationExp(new VariableExp("i"), SelfOp.OP_SELF_INCREASE, true),
                UnopToken.TK_PLUS_PLUS, new VariableToken("i"));
    }

    @Test
    // --i
    public void selfDecreasePreorderParses() throws ParseException {
        assertParses(new SelfOperationExp(new VariableExp("i"), SelfOp.OP_SELF_DECREASE, true),
                UnopToken.TK_MINUS_MINUS, new VariableToken("i"));
    }

    @Test
    // i++
    public void selfIncreaseNoPreorderParses() throws ParseException {
        assertParses(new SelfOperationExp(new VariableExp("i"), SelfOp.OP_SELF_INCREASE, false),
                new VariableToken("i"), UnopToken.TK_PLUS_PLUS);
    }

    @Test
    // i++ + 2
    public void selfDecreaseInAdditiveParses() throws ParseException {
        assertParses(new AdditiveExp(new SelfOperationExp(new VariableExp("i"), SelfOp.OP_SELF_INCREASE, false),
                new IntExp(2), AdditiveOp.EXP_PLUS),
                new VariableToken("i"), UnopToken.TK_PLUS_PLUS, BinopToken.TK_PLUS, new IntToken(2));
    }

    @Test
    // 2 + ++i
    public void selfDecreaseInAdditiveRightParses() throws ParseException {
        assertParses( new AdditiveExp(new IntExp(2), new SelfOperationExp(new VariableExp("i"), SelfOp.OP_SELF_INCREASE, true),
                        AdditiveOp.EXP_PLUS),
                new IntToken(2), BinopToken.TK_PLUS, UnopToken.TK_PLUS_PLUS,  new VariableToken("i"));
    }

    @Test
    // search(x)
    public void funcInstanceWithSingleParameterParses() throws ParseException {
        List<Exp> parameterList = new ArrayList<>();
        parameterList.add(new VariableExp("x"));
        assertParses(new FunctionInstanceExp(new VariableExp("search"), parameterList),
                new VariableToken("search"), BracketsToken.TK_LPAREN, new VariableToken("x"),
                BracketsToken.TK_RPAREN);
    }

    @Test
    // search()
    public void funcInstanceWithNoParameterParses() throws ParseException {
        List<Exp> parameterList = new ArrayList<>();
        assertParses(new FunctionInstanceExp(new VariableExp("search"), parameterList),
                new VariableToken("search"), BracketsToken.TK_LPAREN,
                BracketsToken.TK_RPAREN);
    }

    @Test
    // search(array, 3)
    public void funcInstanceWithTwoParameterParses() throws ParseException {
        List<Exp> parameterList = new ArrayList<>();
        parameterList.add(new VariableExp("array"));
        parameterList.add(new IntExp(3));
        assertParses(new FunctionInstanceExp(new VariableExp("search"), parameterList),
                new VariableToken("search"), BracketsToken.TK_LPAREN, new VariableToken("array"),
                SymbolToken.TK_COMMA, new IntToken(3), BracketsToken.TK_RPAREN);
    }

    @Test
    // arrayOf(1,2,3,"abc",5)
    public void arrayOfParses() throws ParseException {
        List<Exp> expList = new ArrayList<>();
        expList.add(new IntExp(1));
        expList.add(new IntExp(2));
        expList.add(new IntExp(3));
        expList.add(new StringExp("abc", null));
        expList.add(new IntExp(5));
        assertParses(new ArrayOfExp(expList),
                KeywordToken.TK_ARRAY_OF, BracketsToken.TK_LPAREN, new IntToken(1), SymbolToken.TK_COMMA,
                new IntToken(2), SymbolToken.TK_COMMA, new IntToken(3), SymbolToken.TK_COMMA,
                new StringToken("abc"), SymbolToken.TK_COMMA, new IntToken(5), BracketsToken.TK_RPAREN);
    }

    @Test
    // mutableListOf(1,2,3,"abc",5)
    public void mutableListOfParses() throws ParseException {
        List<Exp> expList = new ArrayList<>();
        expList.add(new IntExp(1));
        expList.add(new IntExp(2));
        expList.add(new IntExp(3));
        expList.add(new StringExp("abc", null));
        expList.add(new IntExp(5));
        assertParses(new MutableListOfExp(expList),
                KeywordToken.TK_MUTABLE_LIST_OF, BracketsToken.TK_LPAREN, new IntToken(1), SymbolToken.TK_COMMA,
                new IntToken(2), SymbolToken.TK_COMMA, new IntToken(3), SymbolToken.TK_COMMA,
                new StringToken("abc"), SymbolToken.TK_COMMA, new IntToken(5), BracketsToken.TK_RPAREN);
    }

    @Test
    // {a: Int, b: Int -> a + b}
    public void lambdaExpParses() throws ParseException {
        LinkedHashMap<Exp, Type> parameterList = new LinkedHashMap<Exp, Type>();
        parameterList.put(new VariableExp("a"), BasicType.TYPE_INT);
        parameterList.put(new VariableExp("b"), BasicType.TYPE_INT);
        assertParses(new LambdaExp(parameterList, new AdditiveExp(new VariableExp("a"),
                        new VariableExp("b"), AdditiveOp.EXP_PLUS)),
                BracketsToken.TK_LCURLY, new VariableToken("a"), SymbolToken.TK_COLON,
                TypeToken.TK_TYPE_INT, SymbolToken.TK_COMMA, new VariableToken("b"), SymbolToken.TK_COLON,
                TypeToken.TK_TYPE_INT, SymbolToken.TK_ARROW, new VariableToken("a"), BinopToken.TK_PLUS,
                new VariableToken("b"), BracketsToken.TK_RCURLY);
    }

    @Test
    // var b = 3; {a: Int -> a + b}
    public void lambdaExpWithSingleParameterParses() throws ParseException {
        LinkedHashMap<Exp, Type> parameterList = new LinkedHashMap<Exp, Type>();
        parameterList.put(new VariableExp("a"), BasicType.TYPE_INT);
        assertParses(new LambdaExp(parameterList, new AdditiveExp(new VariableExp("a"),
                        new VariableExp("b"), AdditiveOp.EXP_PLUS)),
                BracketsToken.TK_LCURLY, new VariableToken("a"), SymbolToken.TK_COLON,
                TypeToken.TK_TYPE_INT, SymbolToken.TK_ARROW, new VariableToken("a"), BinopToken.TK_PLUS,
                new VariableToken("b"), BracketsToken.TK_RCURLY);
    }

    @Test
    // val a = 2; var b = 3; { -> a + b}
    public void lambdaExpWithNoParameterParses() throws ParseException {
        LinkedHashMap<Exp, Type> parameterList = new LinkedHashMap<Exp, Type>();
        assertParses(new LambdaExp(parameterList, new AdditiveExp(new VariableExp("a"),
                        new VariableExp("b"), AdditiveOp.EXP_PLUS)),
                BracketsToken.TK_LCURLY,  SymbolToken.TK_ARROW, new VariableToken("a"), BinopToken.TK_PLUS,
                new VariableToken("b"), BracketsToken.TK_RCURLY);
    }

    @Test
    // Array(10, {i -> "s" + i * 2})
    public void arrayExpParses() throws ParseException {
        LinkedHashMap<Exp, Type> parameterList = new LinkedHashMap<Exp, Type>();
        parameterList.put(new VariableExp("i"), null);
        assertParses(new ArrayExp(new IntExp(10), new LambdaExp(parameterList,
                new AdditiveExp(new StringExp("s", null),
                        new MultiplicativeExp(new VariableExp("i"), new IntExp(2),
                                MultiplicativeOp.OP_MULTIPLY), AdditiveOp.EXP_PLUS))),
                TypeToken.TK_ARRAY, BracketsToken.TK_LPAREN, new IntToken(10),
                SymbolToken.TK_COMMA, BracketsToken.TK_LCURLY, new VariableToken("i"),
                SymbolToken.TK_ARROW, new StringToken("s"), BinopToken.TK_PLUS,
                new VariableToken("i"), BinopToken.TK_MULTIPLY, new IntToken(2),
                BracketsToken.TK_RCURLY, BracketsToken.TK_RPAREN);
    }

    @Test
    // MutableList(10, {i -> "s" + i * 2})
    public void mutableListExpParses() throws ParseException {
        LinkedHashMap<Exp, Type> parameterList = new LinkedHashMap<Exp, Type>();
        parameterList.put(new VariableExp("i"), null);
        assertParses(new MutableListExp(new IntExp(10), new LambdaExp(parameterList,
                        new AdditiveExp(new StringExp("s", null),
                                new MultiplicativeExp(new VariableExp("i"), new IntExp(2),
                                        MultiplicativeOp.OP_MULTIPLY), AdditiveOp.EXP_PLUS))),
                TypeToken.TK_MUTABLE_LIST, BracketsToken.TK_LPAREN, new IntToken(10),
                SymbolToken.TK_COMMA, BracketsToken.TK_LCURLY, new VariableToken("i"),
                SymbolToken.TK_ARROW, new StringToken("s"), BinopToken.TK_PLUS,
                new VariableToken("i"), BinopToken.TK_MULTIPLY, new IntToken(2),
                BracketsToken.TK_RCURLY, BracketsToken.TK_RPAREN);
    }

    @Test
    public void plusParses() throws ParseException {
        assertParses(new AdditiveExp(new IntExp(1), new IntExp(2), AdditiveOp.EXP_PLUS),
                new IntToken(1),
                BinopToken.TK_PLUS,
                new IntToken(2));
    }

    @Test
    // !flag
    public void notVariableParses() throws ParseException {
        assertParses(new NotExp(new VariableExp("flag")), UnopToken.TK_NOT, new VariableToken("flag"));
    }

    @Test
    // !(a > 3 + 2 * 4 && false)
    public void notBilogicalParses() throws ParseException {
        assertParses(new NotExp(new BiLogicalExp(new ComparableExp(new VariableExp("a"), new AdditiveExp(new IntExp(3),
                        new MultiplicativeExp(new IntExp(2), new IntExp(4), MultiplicativeOp.OP_MULTIPLY),
                        AdditiveOp.EXP_PLUS), ComparableOp.OP_GREATER_THAN), new BooleanExp(false), BiLogicalOp.OP_AND)),
                UnopToken.TK_NOT, BracketsToken.TK_LPAREN, new VariableToken("a"), BinopToken.TK_GREATER_THAN, new IntToken(3), BinopToken.TK_PLUS,
                new IntToken(2), BinopToken.TK_MULTIPLY, new IntToken(4), BinopToken.TK_AND, KeywordToken.TK_FALSE, BracketsToken.TK_RPAREN);
    }

    @Test
    // 2 + !1 > 3
    public void notPrimaryParses() throws ParseException {
        assertParses(new ComparableExp(new AdditiveExp(new IntExp(2), new NotExp(new IntExp(1)), AdditiveOp.EXP_PLUS),
                new IntExp(3), ComparableOp.OP_GREATER_THAN),
                new IntToken(2), BinopToken.TK_PLUS, UnopToken.TK_NOT, new IntToken(1), BinopToken.TK_GREATER_THAN,
                new IntToken(3));
    }

    @Test
    // flag || 1 + !false
    public void bilogicalNotParses() throws ParseException {
        assertParses(new BiLogicalExp(new VariableExp("flag"), new AdditiveExp(new IntExp(1),
                new NotExp(new BooleanExp(false)), AdditiveOp.EXP_PLUS), BiLogicalOp.OP_OR),
                new VariableToken("flag"), BinopToken.TK_OR, new IntToken(1), BinopToken.TK_PLUS,
                UnopToken.TK_NOT, KeywordToken.TK_FALSE);
    }

    @Test
    // a > 3 + 2 * 4 && false
    public void bilogicalParses() throws ParseException {
        assertParses(new BiLogicalExp(new ComparableExp(new VariableExp("a"), new AdditiveExp(new IntExp(3),
                new MultiplicativeExp(new IntExp(2), new IntExp(4), MultiplicativeOp.OP_MULTIPLY),
                AdditiveOp.EXP_PLUS), ComparableOp.OP_GREATER_THAN), new BooleanExp(false), BiLogicalOp.OP_AND),
                new VariableToken("a"), BinopToken.TK_GREATER_THAN, new IntToken(3), BinopToken.TK_PLUS,
                new IntToken(2), BinopToken.TK_MULTIPLY, new IntToken(4), BinopToken.TK_AND, KeywordToken.TK_FALSE);
    }

    @Test
    // 1 * 2 %3 - 4 / 5
    public void additiveAndMultiplicativeParses() throws ParseException {
        assertParses(new AdditiveExp(new MultiplicativeExp(new MultiplicativeExp(new IntExp(1), new IntExp(2), MultiplicativeOp.OP_MULTIPLY),
                new IntExp(3), MultiplicativeOp.OP_MOD), new MultiplicativeExp(new IntExp(4),
                new IntExp(5), MultiplicativeOp.OP_DIVIDE), AdditiveOp.EXP_MINUS),
                new IntToken(1),
                BinopToken.TK_MULTIPLY,
                new IntToken(2),
                BinopToken.TK_MOD,
                new IntToken(3),
                BinopToken.TK_MINUS,
                new IntToken(4),
                BinopToken.TK_DIVIDE,
                new IntToken(5));
    }

    @Test
    // index > 1 + 2 * 3 + 4
    public void variableCompareParses() throws ParseException {
        assertParses(new ComparableExp(new VariableExp("index"), new AdditiveExp(new AdditiveExp(
                new IntExp(1),
                new MultiplicativeExp(new IntExp(2), new IntExp(3), MultiplicativeOp.OP_MULTIPLY),
                AdditiveOp.EXP_PLUS), new IntExp(4), AdditiveOp.EXP_PLUS), ComparableOp.OP_GREATER_THAN),
                new VariableToken("index"),
                BinopToken.TK_GREATER_THAN,
                new IntToken(1),
                BinopToken.TK_PLUS,
                new IntToken(2),
                BinopToken.TK_MULTIPLY,
                new IntToken(3),
                BinopToken.TK_PLUS,
                new IntToken(4));
    }

    @Test
    public void stringParses() throws ParseException {
        LinkedHashMap<Integer, Exp> map = new LinkedHashMap();
        map.put(5, new AdditiveExp(new VariableExp("a"), new VariableExp("b"), AdditiveOp.EXP_PLUS));
        map.put(12, new VariableExp("beer"));
        map.put(32, new AdditiveExp(new VariableExp("a"), new VariableExp("beer"), AdditiveOp.EXP_PLUS));
        assertParses(new StringExp("a is , b is , c is $ c, sum is ", map),
                new StringToken("a is ${a + b}, b is $beer, c is $ c, sum is ${a + beer}"));
    }

    @Test
    public void plusIsLeftAssociative() throws ParseException {
        assertParses(new AdditiveExp(new AdditiveExp(new IntExp(1),
                        new IntExp(2), AdditiveOp.EXP_PLUS),
                        new IntExp(3), AdditiveOp.EXP_PLUS),
                new IntToken(1),
                BinopToken.TK_PLUS,
                new IntToken(2),
                BinopToken.TK_PLUS,
                new IntToken(3));
    }

    @Test
    public void missingIntegerGivesParseError() {
        assertExpectedException(null,
                new IntToken(1),
                BinopToken.TK_PLUS);
    }
}