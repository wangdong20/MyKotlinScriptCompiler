import com.github.wangdong20.kotlinscriptcompiler.parser.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.*;
import com.github.wangdong20.kotlinscriptcompiler.token.*;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    public static void assertParses(final Exp expected,
                                    final Token... tokens) throws ParseException {
        assertEquals(expected, (new Parser(tokens)).parseToplevelExp());
    } // assertParses

    public static void assertExpectedException(final Exp expected, final Token... tokens) {
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
    public void plusParses() throws ParseException {
        assertParses(new AdditiveExp(new IntExp(1), new IntExp(2), AdditiveOp.EXP_PLUS),
                new IntToken(1),
                BinopToken.TK_PLUS,
                new IntToken(2));
    }

    @Test
    public void notVariableParses() throws ParseException {
        assertParses(new NotExp(new VariableExp("flag")), UnopToken.TK_NOT, new VariableToken("flag"));
    }

    @Test
    public void notBilogicalParses() throws ParseException {
        assertParses(new NotExp(new BiLogicalExp(new ComparableExp(new VariableExp("a"), new AdditiveExp(new IntExp(3),
                        new MultiplicativeExp(new IntExp(2), new IntExp(4), MultiplicativeOp.OP_MULTIPLY),
                        AdditiveOp.EXP_PLUS), ComparableOp.OP_GREATER_THAN), new BooleanExp(false), BiLogicalOp.OP_AND)),
                UnopToken.TK_NOT, BracketsToken.TK_LPAREN, new VariableToken("a"), BinopToken.TK_GREATER_THAN, new IntToken(3), BinopToken.TK_PLUS,
                new IntToken(2), BinopToken.TK_MULTIPLY, new IntToken(4), BinopToken.TK_AND, KeywordToken.TK_FALSE, BracketsToken.TK_RPAREN);
    }

    @Test
    public void notPrimaryParses() throws ParseException {
        assertParses(new ComparableExp(new AdditiveExp(new IntExp(2), new NotExp(new IntExp(1)), AdditiveOp.EXP_PLUS),
                new IntExp(3), ComparableOp.OP_GREATER_THAN),
                new IntToken(2), BinopToken.TK_PLUS, UnopToken.TK_NOT, new IntToken(1), BinopToken.TK_GREATER_THAN,
                new IntToken(3));
    }

    @Test
    public void bilogicalNotParses() throws ParseException {
        assertParses(new BiLogicalExp(new VariableExp("flag"), new AdditiveExp(new IntExp(1),
                new NotExp(new BooleanExp(false)), AdditiveOp.EXP_PLUS), BiLogicalOp.OP_OR),
                new VariableToken("flag"), BinopToken.TK_OR, new IntToken(1), BinopToken.TK_PLUS,
                UnopToken.TK_NOT, KeywordToken.TK_FALSE);
    }

    @Test
    public void bilogicalParses() throws ParseException {
        assertParses(new BiLogicalExp(new ComparableExp(new VariableExp("a"), new AdditiveExp(new IntExp(3),
                new MultiplicativeExp(new IntExp(2), new IntExp(4), MultiplicativeOp.OP_MULTIPLY),
                AdditiveOp.EXP_PLUS), ComparableOp.OP_GREATER_THAN), new BooleanExp(false), BiLogicalOp.OP_AND),
                new VariableToken("a"), BinopToken.TK_GREATER_THAN, new IntToken(3), BinopToken.TK_PLUS,
                new IntToken(2), BinopToken.TK_MULTIPLY, new IntToken(4), BinopToken.TK_AND, KeywordToken.TK_FALSE);
    }

    @Test
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