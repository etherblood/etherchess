package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Castling;
import com.etherblood.etherchess.engine.util.Direction;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.PieceSquareSet;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;
import java.util.function.Consumer;

public class LegalMoveGenerator {

    public static final Move LARGE_CASTLING = new Move(Move.CASTLING, Piece.KING, Square.E1, Square.C1);
    public static final Move SMALL_CASTLING = new Move(Move.CASTLING, Piece.KING, Square.E1, Square.G1);

    public void generatePseudoLegalMoves(State state, Consumer<Move> out) {
        long kingDangerSquares = kingDangerSquares(state);
        long pinnedMask = 0;
        long pushMask = ~0;
        long captureMask = ~0;
        kingMoves(state, kingDangerSquares, out);
        pawnMoves(state, state.pawns() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        knightMoves(state, state.knights() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        bishopMoves(state, state.bishops() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        rookMoves(state, state.rooks() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        queenMoves(state, state.queens() & state.own() & ~pinnedMask, pushMask, captureMask, out);
    }

    public void generateLegalMoves(State state, Consumer<Move> out) {
        long kingDangerSquares = kingDangerSquares(state);

        kingMoves(state, kingDangerSquares, out);

        long captureMask = ~0;
        long pushMask = ~0;
        long ownKings = state.kings() & state.own();
        if ((ownKings & kingDangerSquares) != 0) {
            long checkers = findOpponentCheckers(state);
            int checkerCount = Long.bitCount(checkers);
            if (checkerCount > 1) {
                // more than 1 opponent piece is giving check, we can only evade it with king moves
                return;
            }
            assert checkerCount == 1;
            captureMask = checkers;
            if ((checkers & (state.pawns() | state.knights())) != 0) {
                pushMask = 0;
            } else {
                pushMask = PieceSquareSet.raySquaresBetween(Square.firstOf(ownKings), Square.firstOf(checkers));
            }
        }

        long pinnedMask = 0;
        {
            //pin calculations
            pinnedMask |= handlePinnedMoves(state, Direction.NORTH, state.rooks() | state.queens(), pushMask, captureMask, out);
            pinnedMask |= handlePinnedMoves(state, Direction.EAST, state.rooks() | state.queens(), pushMask, captureMask, out);
            pinnedMask |= handlePinnedMoves(state, Direction.SOUTH, state.rooks() | state.queens(), pushMask, captureMask, out);
            pinnedMask |= handlePinnedMoves(state, Direction.WEST, state.rooks() | state.queens(), pushMask, captureMask, out);

            pinnedMask |= handlePinnedMoves(state, Direction.NORTH_EAST, state.bishops() | state.queens(), pushMask, captureMask, out);
            pinnedMask |= handlePinnedMoves(state, Direction.SOUTH_EAST, state.bishops() | state.queens(), pushMask, captureMask, out);
            pinnedMask |= handlePinnedMoves(state, Direction.SOUTH_WEST, state.bishops() | state.queens(), pushMask, captureMask, out);
            pinnedMask |= handlePinnedMoves(state, Direction.NORTH_WEST, state.bishops() | state.queens(), pushMask, captureMask, out);

        }

        pawnMoves(state, state.pawns() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        knightMoves(state, state.knights() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        bishopMoves(state, state.bishops() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        rookMoves(state, state.rooks() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        queenMoves(state, state.queens() & state.own() & ~pinnedMask, pushMask, captureMask, out);
    }

    private long kingDangerSquares(State state) {
        long kingDangerSquares;
        {
            long ownKingless = state.own() & ~state.kings();
            long occupiedOwnKingExcluded = state.opp() | ownKingless;
            long result = 0;
            long pawns = state.pawns() & state.opp();
            result |= (pawns >>> 7) & ~SquareSet.FILE_A;
            result |= (pawns >>> 9) & ~SquareSet.FILE_H;
            long queens = state.queens() & state.opp();
            while (queens != 0) {
                int from = Square.firstOf(queens);
                result |= PieceSquareSet.queenRays(from, occupiedOwnKingExcluded);
                queens ^= SquareSet.of(from);
            }
            long rooks = state.rooks() & state.opp();
            while (rooks != 0) {
                int from = Square.firstOf(rooks);
                result |= PieceSquareSet.rookRays(from, occupiedOwnKingExcluded);
                rooks ^= SquareSet.of(from);
            }
            long bishops = state.bishops() & state.opp();
            while (bishops != 0) {
                int from = Square.firstOf(bishops);
                result |= PieceSquareSet.bishopRays(from, occupiedOwnKingExcluded);
                bishops ^= SquareSet.of(from);
            }
            long knights = state.knights() & state.opp();
            while (knights != 0) {
                int from = Square.firstOf(knights);
                result |= PieceSquareSet.knightMoves(from);
                knights ^= SquareSet.of(from);
            }
            long kings = state.kings() & state.opp();
            while (kings != 0) {
                int from = Square.firstOf(kings);
                result |= PieceSquareSet.kingMoves(from);
                kings ^= SquareSet.of(from);
            }
            kingDangerSquares = result;
        }
        return kingDangerSquares;
    }

    public long findOwnCheckers(State state) {
        return findOwnAttackers(state, Square.firstOf(state.kings() & state.opp()));
    }

    public long findOpponentCheckers(State state) {
        return findOpponentAttackers(state, Square.firstOf(state.kings() & state.own()));
    }

    public long findOwnAttackers(State state, int target) {
        long targetSquares = SquareSet.of(target);
        long pawnsMask = ((targetSquares >>> 7) & ~SquareSet.FILE_A) | ((targetSquares >>> 9) & ~SquareSet.FILE_H);
        return findAttackers(state, target, pawnsMask, state.own());
    }

    public long findOpponentAttackers(State state, int target) {
        long targetSquares = SquareSet.of(target);
        long pawnsMask = ((targetSquares << 7) & ~SquareSet.FILE_H) | ((targetSquares << 9) & ~SquareSet.FILE_A);
        return findAttackers(state, target, pawnsMask, state.opp());
    }

    private long findAttackers(State state, int target, long pawnsMask, long attackerMask) {
        long attackers = 0;
        attackers |= PieceSquareSet.knightMoves(target) & attackerMask & state.knights();
        attackers |= pawnsMask & attackerMask & state.pawns();
        attackers |= PieceSquareSet.rookRays(target, state.occupied()) & attackerMask & (state.rooks() | state.queens());
        attackers |= PieceSquareSet.bishopRays(target, state.occupied()) & attackerMask & (state.bishops() | state.queens());
        return attackers;
    }

    private long handlePinnedMoves(State state, int direction, long attackersMask, long pushMask, long captureMask, Consumer<Move> out) {
        long pinnedSet = 0;
        long ownKings = state.kings() & state.own();
        int kingSquare = Square.firstOf(ownKings);
        long ray = SquareSet.simpleDirectionRay(direction, kingSquare);
        if ((state.own() & ray) != 0 && (state.opp() & ray & attackersMask) != 0) {
            long kingRay = PieceSquareSet.directionRay(direction, kingSquare, state.occupied());
            long pinned = kingRay & state.own();
            if (pinned != 0) {
                assert Long.bitCount(pinned) == 1;
                int from = Square.firstOf(pinned);
                long pinRay = PieceSquareSet.directionRay(direction, from, state.occupied());
                if ((pinRay & attackersMask & state.opp()) != 0) {
                    if ((pinned & state.rooks()) != 0 && !Direction.isDiagonal(direction)) {
                        long slides = pinRay | (kingRay ^ pinned);
                        generateDefaultMoves(Piece.ROOK, from, slides & ~state.own() & (pushMask | captureMask), out);
                    } else if ((pinned & state.queens()) != 0) {
                        long slides = pinRay | (kingRay ^ pinned);
                        generateDefaultMoves(Piece.QUEEN, from, slides & ~state.own() & (pushMask | captureMask), out);
                    } else if ((pinned & state.bishops()) != 0 && Direction.isDiagonal(direction)) {
                        long slides = pinRay | (kingRay ^ pinned);
                        generateDefaultMoves(Piece.BISHOP, from, slides & ~state.own() & (pushMask | captureMask), out);
                    } else if ((pinned & state.pawns()) != 0) {
                        pawnMoves(state, pinned, pushMask & ray, captureMask & ray, out);
                    } else {
                        // pinned knights can never move
                    }

                    pinnedSet |= pinned;
                }
            }
        }
        return pinnedSet;
    }

    private void queenMoves(State state, long ownQueens, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownQueens != 0) {
            int from = Square.firstOf(ownQueens);

            long occupied = state.own() | state.opp();
            long queenSlides = PieceSquareSet.queenRays(from, occupied);
            generateDefaultMoves(Piece.QUEEN, from, queenSlides & ~state.own() & (pushMask | captureMask), out);

            ownQueens ^= SquareSet.of(from);
        }
    }

    private void rookMoves(State state, long ownRooks, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownRooks != 0) {
            int from = Square.firstOf(ownRooks);

            long occupied = state.own() | state.opp();
            long rookSlides = PieceSquareSet.rookRays(from, occupied);
            generateDefaultMoves(Piece.ROOK, from, rookSlides & ~state.own() & (pushMask | captureMask), out);

            ownRooks ^= SquareSet.of(from);
        }
    }

    private void bishopMoves(State state, long ownBishops, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownBishops != 0) {
            int from = Square.firstOf(ownBishops);

            long occupied = state.own() | state.opp();
            long bishopSlides = PieceSquareSet.bishopRays(from, occupied);
            generateDefaultMoves(Piece.BISHOP, from, bishopSlides & ~state.own() & (pushMask | captureMask), out);

            ownBishops ^= SquareSet.of(from);
        }
    }

    private void knightMoves(State state, long ownKnights, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownKnights != 0) {
            int from = Square.firstOf(ownKnights);

            generateDefaultMoves(Piece.KNIGHT, from, PieceSquareSet.knightMoves(from) & ~state.own() & (pushMask | captureMask), out);

            ownKnights ^= SquareSet.of(from);
        }
    }

    private void kingMoves(State state, long kingDangerSquares, Consumer<Move> out) {
        long ownKings = state.kings() & state.own();
        int from = Square.firstOf(ownKings);
        generateDefaultMoves(Piece.KING, from, PieceSquareSet.kingMoves(from) & ~(state.own() | kingDangerSquares), out);
        if ((ownKings & kingDangerSquares) == 0) {
            if ((state.availableCastlings & Castling.A1) != 0) {
                if (((SquareSet.B1 | SquareSet.C1 | SquareSet.D1) & state.occupied()) == 0) {
                    if (((SquareSet.C1 | SquareSet.D1 | SquareSet.E1) & kingDangerSquares) == 0) {
                        out.accept(LARGE_CASTLING);
                    }
                }
            }
            if ((state.availableCastlings & Castling.H1) != 0) {
                if (((SquareSet.F1 | SquareSet.G1) & state.occupied()) == 0) {
                    if (((SquareSet.E1 | SquareSet.F1 | SquareSet.G1) & kingDangerSquares) == 0) {
                        out.accept(SMALL_CASTLING);
                    }
                }
            }
        }
    }

    private void generateDefaultMoves(int piece, int from, long toSquareSet, Consumer<Move> out) {
        while (toSquareSet != 0) {
            int to = Square.firstOf(toSquareSet);
            out.accept(Move.defaultMove(piece, from, to));

            toSquareSet ^= SquareSet.of(to);
        }
    }

    private void pawnMoves(State state, long ownPawns, long pushMask, long captureMask, Consumer<Move> out) {
        long moves = (ownPawns << 8) & ~state.occupied();
        long doubles = (moves << 8) & ~state.occupied() & SquareSet.RANK_4;
        moves &= pushMask;
        while (moves != 0) {
            int to = Square.firstOf(moves);
            int from = to - 8;
            if (to >= Square.A8) {
                out.accept(Move.promotion(Move.PROMOTION_QUEEN, from, to));
                out.accept(Move.promotion(Move.PROMOTION_KNIGHT, from, to));
                out.accept(Move.promotion(Move.PROMOTION_ROOK, from, to));
                out.accept(Move.promotion(Move.PROMOTION_BISHOP, from, to));
            } else {
                out.accept(Move.pawnMove(from, to));
            }

            moves ^= SquareSet.of(to);
        }
        doubles &= pushMask;
        while (doubles != 0) {
            int to = Square.firstOf(doubles);
            int from = to - 16;
            out.accept(Move.pawnDouble(from, to));

            doubles ^= SquareSet.of(to);
        }

        if (state.enPassantSquare != 0) {
            long pawnMask = (SquareSet.of(state.enPassantSquare - 9) | SquareSet.of(state.enPassantSquare - 7)) & SquareSet.RANK_5;
            long pawns = ownPawns & pawnMask;
            int kingSquare = Square.firstOf(state.kings() & state.own());
            while (pawns != 0) {
                long pawn = SquareSet.firstOf(pawns);
                pawns ^= pawn;
                long occ = state.occupied() ^ (pawn | SquareSet.of(state.enPassantSquare) | SquareSet.of(state.enPassantSquare - 8));
                if ((PieceSquareSet.rookRays(kingSquare, occ) & (state.rooks() | state.queens()) & state.opp()) != 0
                        || (PieceSquareSet.bishopRays(kingSquare, occ) & (state.bishops() | state.queens()) & state.opp()) != 0) {
                    // en passant is pinned and can't be used
                    continue;
                }
                out.accept(Move.enPassant(Square.firstOf(pawn), state.enPassantSquare));
            }
        }

        long leftAttacks = (ownPawns << 7) & ~SquareSet.FILE_H & state.opp() & captureMask;
        while (leftAttacks != 0) {
            int to = Square.firstOf(leftAttacks);
            int from = to - 7;
            if (to >= Square.A8) {
                out.accept(Move.promotion(Move.PROMOTION_QUEEN, from, to));
                out.accept(Move.promotion(Move.PROMOTION_KNIGHT, from, to));
                out.accept(Move.promotion(Move.PROMOTION_ROOK, from, to));
                out.accept(Move.promotion(Move.PROMOTION_BISHOP, from, to));
            } else {
                out.accept(Move.pawnMove(from, to));
            }

            leftAttacks ^= SquareSet.of(to);
        }
        long rightAttacks = (ownPawns << 9) & ~SquareSet.FILE_A & state.opp() & captureMask;
        while (rightAttacks != 0) {
            int to = Square.firstOf(rightAttacks);
            int from = to - 9;
            if (to >= Square.A8) {
                out.accept(Move.promotion(Move.PROMOTION_QUEEN, from, to));
                out.accept(Move.promotion(Move.PROMOTION_KNIGHT, from, to));
                out.accept(Move.promotion(Move.PROMOTION_ROOK, from, to));
                out.accept(Move.promotion(Move.PROMOTION_BISHOP, from, to));
            } else {
                out.accept(Move.pawnMove(from, to));
            }

            rightAttacks ^= SquareSet.of(to);
        }
    }
}
